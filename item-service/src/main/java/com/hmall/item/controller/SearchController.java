package com.hmall.item.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.common.domain.PageDTO;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.query.ItemPageQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@CrossOrigin(origins = "*") // 允许所有来源调用
@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final RestHighLevelClient client;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) {
        try {
            log.info("搜索商品，查询条件：{}", JSONUtil.toJsonStr(query));
            SearchResponse response = doSearchOnce(query, query.getKey());

            // 若用户输入是自然语言（含颜色/颜值等软偏好词），用严格匹配容易 0 命中；这里做一次“降级重试”
            if (isEmpty(response) && StrUtil.isNotBlank(query.getKey())) {
                String degraded = degradeKeyword(query.getKey());
                if (StrUtil.isNotBlank(degraded) && !degraded.equals(query.getKey())) {
                    log.info("搜索结果为空，降级重试：originKey='{}', degradedKey='{}'", query.getKey(), degraded);
                    response = doSearchOnce(query, degraded);
                }
            }
            // 类目存的是「智能手机」等，整词不可能 term 命中「手机」；最后再缩 key 试一次
            if (isEmpty(response) && StrUtil.isNotBlank(query.getKey()) && query.getKey().contains("手机")) {
                log.info("搜索结果仍为空，使用极简关键词重试：key='手机'");
                response = doSearchOnce(query, "手机");
            }

            // 4.解析响应
            SearchHits hits = response.getHits();
            // 4.1.获取总条数
            long total = 0;
            if (hits.getTotalHits() != null) {
                total = hits.getTotalHits().value;
            }
            // 4.2.获取页面数据
            SearchHit[] searchHits = hits.getHits();
            List<ItemDTO> list = new ArrayList<>(searchHits.length);
            for (SearchHit hit : searchHits) {
                // 获取 source
                String json = hit.getSourceAsString();
                // 反序列化
                ItemDTO item = JSONUtil.toBean(json, ItemDTO.class);
                list.add(item);
            }

            return new PageDTO<>(total, (total + query.getPageSize() - 1) / query.getPageSize(), list);
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch 搜索失败", e);
        }
    }

    private SearchResponse doSearchOnce(ItemPageQuery query, String key) throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("items");
        // 2.准备请求参数
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 2.1.构建查询条件（宽召回 + 软偏好加分）
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (StrUtil.isNotBlank(key)) {
            // 过去用 AND 会导致自然语言描述（多词）经常 0 命中；改为 OR，并用 should 召回更多候选
            MatchQueryBuilder allMatch = QueryBuilders.matchQuery("all", key).operator(Operator.OR);
            boolQuery.should(allMatch);
            boolQuery.should(QueryBuilders.matchQuery("name", key).operator(Operator.OR).boost(2.0f));
            boolQuery.should(QueryBuilders.matchQuery("spec", key).operator(Operator.OR).boost(1.8f));
            boolQuery.should(QueryBuilders.matchQuery("specColor", key).operator(Operator.OR).boost(2.3f));
            boolQuery.should(QueryBuilders.matchQuery("specSize", key).operator(Operator.OR).boost(1.0f));
            boolQuery.should(QueryBuilders.matchQuery("brand", key).operator(Operator.OR).boost(1.2f));
            boolQuery.should(QueryBuilders.matchQuery("category", key).operator(Operator.OR).boost(1.2f));

            // category 为 keyword 时，term「手机」命不中「智能手机」；用 wildcard 做子串匹配
            if (key.contains("手机") && StrUtil.isBlank(query.getCategory())) {
                boolQuery.should(QueryBuilders.wildcardQuery("category", "*手机*").caseInsensitive(true).boost(2.5f));
            }
            boolQuery.minimumShouldMatch(1);
        } else {
            boolQuery.must(QueryBuilders.matchAllQuery());
        }

        // 默认仅上架（与同步 ES 的 status=1 一致）；显式传 status 可查其它状态
        int statusFilter = query.getStatus() != null ? query.getStatus() : 1;
        boolQuery.filter(QueryBuilders.termQuery("status", statusFilter));

        // 品牌过滤
        if (StrUtil.isNotBlank(query.getBrand())) {
            boolQuery.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        // 分类过滤
        if (StrUtil.isNotBlank(query.getCategory())) {
            boolQuery.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        // 价格范围过滤
        if (query.getMinPrice() != null && query.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
        }

        sourceBuilder.query(boolQuery);

        // 2.2.分页
        sourceBuilder.from(query.from());
        sourceBuilder.size(query.getPageSize());

        // 2.3.排序
        if (StrUtil.isNotBlank(query.getSortBy())) {
            sourceBuilder.sort(query.getSortBy(), query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
        } else {
            sourceBuilder.sort("updateTime", SortOrder.DESC);
        }

        request.source(sourceBuilder);
        return client.search(request, RequestOptions.DEFAULT);
    }

    private boolean isEmpty(SearchResponse response) {
        if (response == null || response.getHits() == null || response.getHits().getTotalHits() == null) {
            return true;
        }
        return response.getHits().getTotalHits().value <= 0;
    }

    /**
     * 降级关键词：移除颜色/颜值等“软偏好词”，保留品类/品牌/核心名词，提高召回。
     */
    private String degradeKeyword(String key) {
        if (key == null) return "";
        String k = key;
        // 常见颜色词
        String[] softWords = {
                "金色", "土豪金", "香槟金", "银色", "黑色", "白色", "红色", "蓝色", "绿色", "紫色", "粉色", "灰色", "深空灰",
                "颜值", "高颜值", "好看", "漂亮", "外观", "质感", "高级"
        };
        for (String w : softWords) {
            k = k.replace(w, " ");
        }
        // 简单清理标点与多空格
        k = k.replaceAll("[\\p{Punct}，。！？、；：\\s]+", " ").trim();
        return k;
    }
}
