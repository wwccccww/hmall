package com.hmall.item.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.common.catalog.ItemCategoryCatalog;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*")
@Api(tags = "\u5546\u54c1\u641c\u7d22")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final RestHighLevelClient client;

    @ApiOperation("\u5546\u54c1\u641c\u7d22")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) {
        try {
            log.info("\u5546\u54c1\u641c\u7d22\u6761\u4ef6\uff1a{}", JSONUtil.toJsonStr(query));
            SearchResponse response = doSearchOnce(query, query.getKey());

            if (isEmpty(response) && StrUtil.isNotBlank(query.getKey())) {
                String degraded = degradeKeyword(query.getKey());
                if (StrUtil.isNotBlank(degraded) && !degraded.equals(query.getKey())) {
                    log.info("\u641c\u7d22\u65e0\u7ed3\u679c\uff0c\u964d\u7ea7\u5173\u952e\u8bcd\uff1aoriginKey='{}', degradedKey='{}'", query.getKey(), degraded);
                    response = doSearchOnce(query, degraded);
                }
            }
            if (isEmpty(response) && StrUtil.isNotBlank(query.getKey()) && query.getKey().contains("\u66f2\u9762\u7535\u89c6")) {
                log.info("\u641c\u7d22\u65e0\u7ed3\u679c\uff0c\u66f2\u9762\u7535\u89c6\u5bbd\u677e\u4e3a\uff1akey='\u7535\u89c6'");
                response = doSearchOnce(query, "\u7535\u89c6");
            }
            if (isEmpty(response) && StrUtil.isNotBlank(query.getKey())
                    && (query.getKey().contains("\u978b") || query.getKey().contains("\u9774"))
                    && StrUtil.isBlank(query.getCategory())) {
                String shoeKey = query.getKey().contains("\u9774") ? "\u9774" : "\u978b";
                log.info("\u641c\u7d22\u65e0\u7ed3\u679c\uff0c\u978b\u7c7b\u515a\u5e95\uff1akey='{}'", shoeKey);
                response = doSearchOnce(query, shoeKey);
            }

            SearchHits hits = response.getHits();
            long total = 0;
            if (hits.getTotalHits() != null) {
                total = hits.getTotalHits().value;
            }
            SearchHit[] searchHits = hits.getHits();
            List<ItemDTO> list = new ArrayList<>(searchHits.length);
            for (SearchHit hit : searchHits) {
                String json = hit.getSourceAsString();
                ItemDTO item = JSONUtil.toBean(json, ItemDTO.class);
                list.add(item);
            }

            return new PageDTO<>(total, (total + query.getPageSize() - 1) / query.getPageSize(), list);
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch \u67e5\u8be2\u5931\u8d25", e);
        }
    }

    private SearchResponse doSearchOnce(ItemPageQuery query, String key) throws IOException {
        SearchRequest request = new SearchRequest("items");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (StrUtil.isNotBlank(key)) {
            boolQuery.should(QueryBuilders.matchQuery("all", key).operator(Operator.OR));
            boolQuery.should(QueryBuilders.matchQuery("name", key).operator(Operator.OR).boost(2.0f));
            boolQuery.should(QueryBuilders.matchQuery("spec", key).operator(Operator.OR).boost(1.8f));
            boolQuery.should(QueryBuilders.matchQuery("specColor", key).operator(Operator.OR).boost(2.3f));
            boolQuery.should(QueryBuilders.matchQuery("specSize", key).operator(Operator.OR).boost(1.0f));
            boolQuery.should(QueryBuilders.matchQuery("brand", key).operator(Operator.OR).boost(1.2f));
            boolQuery.should(QueryBuilders.matchQuery("category", key).operator(Operator.OR).boost(1.2f));
            String colorKw = ItemCategoryCatalog.inferColorKeyword(key);
            if (StrUtil.isNotBlank(colorKw)) {
                boolQuery.should(QueryBuilders.matchQuery("name", colorKw).operator(Operator.OR).boost(4.0f));
                boolQuery.should(QueryBuilders.matchQuery("specColor", colorKw).operator(Operator.OR).boost(3.5f));
            }
            boolQuery.minimumShouldMatch(1);
        } else {
            boolQuery.must(QueryBuilders.matchAllQuery());
        }

        int statusFilter = query.getStatus() != null ? query.getStatus() : 1;
        boolQuery.filter(QueryBuilders.termQuery("status", statusFilter));

        if (StrUtil.isNotBlank(query.getBrand())) {
            boolQuery.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            boolQuery.filter(QueryBuilders.termQuery("category", query.getCategory()));
        } else {
            applyCategoryIntentFilters(boolQuery, key);
        }
        if (query.getMinPrice() != null && query.getMaxPrice() != null) {
            boolQuery.filter(
                    QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
        }
        if (StrUtil.isNotBlank(query.getSpecColor())) {
            boolQuery.filter(QueryBuilders.matchQuery("specColor", query.getSpecColor()).operator(Operator.OR));
        }
        if (StrUtil.isNotBlank(query.getSpecSize())) {
            boolQuery.filter(QueryBuilders.matchQuery("specSize", query.getSpecSize()).operator(Operator.OR));
        }

        sourceBuilder.query(boolQuery);

        sourceBuilder.from(query.from());
        sourceBuilder.size(query.getPageSize());

        if (StrUtil.isNotBlank(query.getSortBy())) {
            sourceBuilder.sort(query.getSortBy(), query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
        } else {
            sourceBuilder.sort("updateTime", SortOrder.DESC);
        }

        request.source(sourceBuilder);
        return client.search(request, RequestOptions.DEFAULT);
    }

    /**
     * \u672a\u6307\u5b9a\u7cbe\u786e category \u65f6\uff0c\u7528\u4e0e\u5e93\u8868\u4e00\u81f4\u7684\u7c7b\u76ee\u767d\u540d\u5355\u505a {@code term} \u8fc7\u6ee4\uff08 keyword \u5b57\u6bb5\u4e0a\u6bd4 wildcard \u66f4\u7a33\uff09\u3002
     */
    private static void applyCategoryIntentFilters(BoolQueryBuilder boolQuery, String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }
        String cat = ItemCategoryCatalog.inferCategoryFromUserText(key);
        if (StrUtil.isNotBlank(cat)) {
            boolQuery.filter(QueryBuilders.termQuery("category", cat));
        }
    }

    private boolean isEmpty(SearchResponse response) {
        if (response == null || response.getHits() == null || response.getHits().getTotalHits() == null) {
            return true;
        }
        return response.getHits().getTotalHits().value <= 0;
    }

    /**
     * \u964d\u7ea7\u641c\u7d22\uff1a\u5254\u9664\u989c\u8272\u7b49\u8bcd\uff0c\u63d0\u9ad8\u4e8c\u6b21\u53ec\u56de\u547d\u4e2d\u7387\u3002
     */
    private String degradeKeyword(String key) {
        if (key == null) {
            return "";
        }
        String k = key;
        String[] softWords = {
                "\u84dd\u8272", "\u7eff\u8272", "\u7ea2\u8272", "\u9ec4\u8272", "\u9ed1\u8272", "\u767d\u8272", "\u7d2b\u8272", "\u7070\u8272", "\u7c89\u8272",
                "\u68d5\u8272", "\u91d1\u8272", "\u94f6\u8272", "\u7c73\u8272", "\u6a59\u8272", "\u6df1\u84dd", "\u6d45\u84dd", "\u58a8\u7eff"
        };
        for (String w : softWords) {
            k = k.replace(w, " ");
        }
        k = k.replaceAll("[\\p{Punct}\uff0c\u3002\uff01\uff1f\u3001\uff1b\uff1a\\s]+", " ").trim();
        return k;
    }
}
