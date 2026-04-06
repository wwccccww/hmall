package com.hmall.ai.config;

import com.hmall.ai.knowledge.KnowledgeProperties;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库专用 ES 客户端（索引与商品索引隔离，仅连集群）。
 */
@Configuration
@ConditionalOnProperty(prefix = "hm.ai.knowledge", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeElasticsearchConfiguration {

    @Bean(destroyMethod = "close")
    public RestHighLevelClient knowledgeElasticsearchClient(KnowledgeProperties knowledgeProperties) {
        return new RestHighLevelClient(RestClient.builder(
                new HttpHost(knowledgeProperties.getEsHost(), knowledgeProperties.getEsPort(), "http")
        ));
    }
}
