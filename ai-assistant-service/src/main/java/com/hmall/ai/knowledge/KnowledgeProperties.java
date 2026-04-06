package com.hmall.ai.knowledge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地 Markdown 知识库 → Elasticsearch 向量索引配置。
 */
@Data
@ConfigurationProperties(prefix = "hm.ai.knowledge")
public class KnowledgeProperties {

    /**
     * 是否启用向量知识库检索（本地 RAG 阶段）。
     */
    private boolean enabled = true;

    /** Elasticsearch 主机（与 item-service 的 ES 同集群即可，索引独立） */
    private String esHost = "localhost";

    private int esPort = 9200;

    /** 知识库向量索引名，与商品索引隔离 */
    private String esIndex = "ai_kb_chunks";

    /** 向量检索 Top-K */
    private int searchTopK = 5;

    /** 写入 prompt 的知识库文本总长上限（字符） */
    private int maxPromptChars = 3600;

    /**
     * 启动时若索引文档数为 0，是否异步触发全量导入（需 Embedding 可用）。
     */
    private boolean autoReindexIfEmpty = true;

    /** 管理接口 POST /ai/admin/reindex-kb 可选校验：非空则要求请求头 X-Admin-Token 一致 */
    private String adminToken = "";
}
