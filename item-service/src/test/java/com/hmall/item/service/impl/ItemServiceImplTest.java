package com.hmall.item.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

// @SpringBootTest(properties = "spring.profiles.active=local")
class ItemServiceImplTest {
    // private RestHighLevelClient client;

    // @Autowired
    // private IItemService itemService;

    // @BeforeEach
    // void setUp() {
    //     this.client = new RestHighLevelClient(RestClient.builder(
    //             HttpHost.create("http://192.168.116.130:9200")
    //     ));
    // }

    // @Test
    // void testConnect() {
    //     System.out.println(client);
    // }

    // @Test
    // void testCreateIndex() throws IOException {
    //     // 1.创建Request对象
    //     CreateIndexRequest request = new CreateIndexRequest("items");
    //     // 2.准备请求参数
    //     request.source(MAPPING_TEMPLATE, XContentType.JSON);
    //     // 3.发送请求
    //     client.indices().create(request, RequestOptions.DEFAULT);
    // }

    // @Test
    // void testDeleteIndex() throws IOException {
    //     // 1.创建Request对象
    //     DeleteIndexRequest request = new DeleteIndexRequest("items");
    //     // 2.发送请求
    //     client.indices().delete(request, RequestOptions.DEFAULT);
    //     System.out.println("索引删除成功！");
    // }

    // @Test
    // void testExistsIndex() throws IOException {
    //     // 1.创建Request对象
    //     GetIndexRequest request = new GetIndexRequest("items");
    //     // 2.发送请求
    //     boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
    //     // 3.输出
    //     System.err.println(exists ? "索引库已经存在！" : "索引库不存在！");
    // }

    // @Test
    // void testBulkExport() throws IOException {
    //     // 1.分批查询数据库数据
    //     int pageNo = 1;
    //     int pageSize = 500;
    //     while (true) {
    //         List<Item> items = itemService.lambdaQuery()
    //                 .eq(Item::getStatus, 1)
    //                 .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNo, pageSize))
    //                 .getRecords();
    //         if (items == null || items.size() == 0) {
    //             break;
    //         }
    //         // 2.准备BulkRequest
    //         BulkRequest request = new BulkRequest();
    //         for (Item item : items) {
    //             // 2.1.转换为ItemDoc
    //             ItemDoc itemDoc = new ItemDoc(item);
    //             // 2.2.创建IndexRequest
    //             request.add(new IndexRequest("items")
    //                     .id(itemDoc.getId().toString())
    //                     .source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON));
    //         }
    //         // 3.发送请求
    //         client.bulk(request, RequestOptions.DEFAULT);
            
    //         System.out.println("成功导入第 " + pageNo + " 页数据，共 " + items.size() + " 条");
    //         pageNo++;
    //     }
    // }

    // @AfterEach
    // void tearDown() throws IOException {
    //     this.client.close();
    // }

    // static final String MAPPING_TEMPLATE = "{\n" +
    //         "  \"mappings\": {\n" +
    //         "    \"properties\": {\n" +
    //         "      \"id\": { \"type\": \"keyword\" },\n" +
    //         "      \"name\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\", \"copy_to\": \"all\" },\n" +
    //         "      \"price\": { \"type\": \"integer\" },\n" +
    //         "      \"stock\": { \"type\": \"integer\" },\n" +
    //         "      \"image\": { \"type\": \"keyword\", \"index\": false },\n" +
    //         "      \"category\": { \"type\": \"keyword\", \"copy_to\": \"all\" },\n" +
    //         "      \"brand\": { \"type\": \"keyword\", \"copy_to\": \"all\" },\n" +
    //         "      \"sold\": { \"type\": \"integer\" },\n" +
    //         "      \"commentCount\": { \"type\": \"integer\" },\n" +
    //         "      \"isAD\": { \"type\": \"boolean\" },\n" +
    //         "      \"updateTime\": { \"type\": \"date\" },\n" +
    //         "      \"all\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\" }\n" +
    //         "    }\n" +
    //         "  }\n" +
    //         "}";
}
