package com.hmall.item.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.List;

/**
 * ES 商品索引：每次同步会先删除 {@code items} 索引再重建（避免旧 mapping 缺字段），然后从 MySQL 全量导入上架商品（status=1）。
 * <p>
 * 前置：<br>
 * 1. 本机/虚拟机已启动 Elasticsearch（默认地址与 {@code hm.es.host} 一致，见 {@code ElasticsearchConfig}）。<br>
 * 2. 已安装 IK 分词插件（mapping 中 name/spec/all 使用 {@code ik_max_word}）；若无 IK，请把 {@link #MAPPING_TEMPLATE} 里对应 analyzer 改为 {@code standard}。<br>
 * 3. {@code spring.profiles.active=local} 下 MySQL {@code hm-item} 有商品数据。
 */
@SpringBootTest(properties = "spring.profiles.active=local")
class ItemServiceImplTest {

    private static final String INDEX = "items";
    private static final int BULK_PAGE_SIZE = 500;

    /**
     * 与 {@link ItemDoc} 及 {@code SearchController} 使用的字段对齐。
     */
    static final String MAPPING_TEMPLATE = "{\n"
            + "  \"mappings\": {\n"
            + "    \"properties\": {\n"
            + "      \"id\": { \"type\": \"long\" },\n"
            + "      \"name\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\" },\n"
            + "      \"price\": { \"type\": \"integer\" },\n"
            + "      \"stock\": { \"type\": \"integer\" },\n"
            + "      \"image\": { \"type\": \"keyword\", \"index\": false },\n"
            + "      \"category\": { \"type\": \"keyword\" },\n"
            + "      \"brand\": { \"type\": \"keyword\" },\n"
            + "      \"spec\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\" },\n"
            + "      \"specColor\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\" },\n"
            + "      \"specSize\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\" },\n"
            + "      \"sold\": { \"type\": \"integer\" },\n"
            + "      \"commentCount\": { \"type\": \"integer\" },\n"
            + "      \"isAD\": { \"type\": \"boolean\" },\n"
            + "      \"status\": { \"type\": \"integer\" },\n"
            + "      \"createTime\": { \"type\": \"date\" },\n"
            + "      \"updateTime\": { \"type\": \"date\" },\n"
            + "      \"creater\": { \"type\": \"long\" },\n"
            + "      \"updater\": { \"type\": \"long\" },\n"
            + "      \"all\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\" }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private IItemService itemService;

    @Test
    void testRedisConnect() {
        stringRedisTemplate.opsForValue().set("cursor:test:key", "Connection_Success");
        String result = stringRedisTemplate.opsForValue().get("cursor:test:key");
        System.out.println("================================");
        System.out.println("Redis result >>> " + result);
        System.out.println("================================");
    }

    @Test
    @DisplayName("删除并重建 items 索引，再将上架商品全量同步到 ES")
    void syncItemsToElasticsearch() throws IOException {
        recreateItemsIndex();
        int pageNo = 1;
        long totalIndexed = 0;
        while (true) {
            List<Item> items = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(new Page<>(pageNo, BULK_PAGE_SIZE))
                    .getRecords();
            if (items == null || items.isEmpty()) {
                break;
            }
            BulkRequest bulk = new BulkRequest();
            for (Item item : items) {
                ItemDoc doc = new ItemDoc(item);
                bulk.add(new IndexRequest(INDEX)
                        .id(doc.getId().toString())
                        .source(JSONUtil.toJsonStr(doc), XContentType.JSON));
            }
            restHighLevelClient.bulk(bulk, RequestOptions.DEFAULT);
            totalIndexed += items.size();
            System.out.println("已写入 ES 第 " + pageNo + " 批，本批 " + items.size() + " 条，累计 " + totalIndexed);
            pageNo++;
        }
        System.out.println("同步完成，共 " + totalIndexed + " 条（仅 status=1）");
    }

    /**
     * 删除旧索引（若存在）后按 {@link #MAPPING_TEMPLATE} 重建，保证与新字段（如 specColor/specSize）一致。
     */
    private void recreateItemsIndex() throws IOException {
        GetIndexRequest exists = new GetIndexRequest(INDEX);
        if (restHighLevelClient.indices().exists(exists, RequestOptions.DEFAULT)) {
            DeleteIndexRequest del = new DeleteIndexRequest(INDEX);
            restHighLevelClient.indices().delete(del, RequestOptions.DEFAULT);
            System.out.println("已删除旧索引 " + INDEX);
        }
        CreateIndexRequest create = new CreateIndexRequest(INDEX);
        create.source(MAPPING_TEMPLATE, XContentType.JSON);
        restHighLevelClient.indices().create(create, RequestOptions.DEFAULT);
        System.out.println("已按最新 mapping 创建索引 " + INDEX);
    }
}
