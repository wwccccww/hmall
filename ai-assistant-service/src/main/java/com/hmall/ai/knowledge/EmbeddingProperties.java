package com.hmall.ai.knowledge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文本向量（OpenAI 兼容 /v1/embeddings），默认对接百炼 compatible-mode。
 */
@Data
@ConfigurationProperties(prefix = "hm.ai.embedding")
public class EmbeddingProperties {

    /**
     * 不含末尾斜杠，例如 https://dashscope.aliyuncs.com/compatible-mode
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode";

    /** 模型名，例如 text-embedding-v2（维度需与 {@link #dims} 一致） */
    private String model = "text-embedding-v2";

    /** API Key；为空时在 {@link com.hmall.ai.knowledge.EmbeddingClient} 内回退 hm.ai.llm.api-key */
    private String apiKey = "";

    /** 向量维度，须与索引 mapping 及模型一致 */
    private int dims = 1536;

    private int timeoutMs = 30000;

    /** 单次 embedding 请求最多条数（避免过大 body） */
    private int batchSize = 8;
}
