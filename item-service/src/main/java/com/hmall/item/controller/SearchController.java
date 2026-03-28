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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            // 1.准备Request
            SearchRequest request = new SearchRequest("items");
            // 2.准备请求参数
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            // 2.1.构建查询条件
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            // 词条查询关键词
            if (StrUtil.isNotBlank(query.getKey())) {
                boolQuery.must(QueryBuilders.matchQuery("all", query.getKey()).operator(Operator.AND));
            } else {
                boolQuery.must(QueryBuilders.matchAllQuery());
            }
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

            // 3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

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
}
