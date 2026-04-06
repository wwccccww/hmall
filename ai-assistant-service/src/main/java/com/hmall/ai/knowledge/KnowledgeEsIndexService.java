package com.hmall.ai.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库向量索引：创建 mapping、bulk 写入、script_score 余弦检索（ES 7.12）。
 */
@Slf4j
@Service
@ConditionalOnBean(RestHighLevelClient.class)
@RequiredArgsConstructor
public class KnowledgeEsIndexService {

    private final RestHighLevelClient esClient;
    private final KnowledgeProperties knowledgeProperties;
    private final EmbeddingProperties embeddingProperties;

    public String indexName() {
        return knowledgeProperties.getEsIndex();
    }

    public boolean indexExists() throws IOException {
        return esClient.indices().exists(new GetIndexRequest(indexName()), RequestOptions.DEFAULT);
    }

    public void createIndexIfNotExists() throws IOException {
        if (indexExists()) {
            return;
        }
        int dims = embeddingProperties.getDims();
        String json = ""
                + "{"
                + "  \"mappings\": {"
                + "    \"properties\": {"
                + "      \"chunk_id\": { \"type\": \"keyword\" },"
                + "      \"source_file\": { \"type\": \"keyword\" },"
                + "      \"text\": { \"type\": \"text\" },"
                + "      \"embedding\": { \"type\": \"dense_vector\", \"dims\": " + dims + " }"
                + "    }"
                + "  }"
                + "}";
        CreateIndexRequest req = new CreateIndexRequest(indexName());
        req.source(json, XContentType.JSON);
        esClient.indices().create(req, RequestOptions.DEFAULT);
        log.info("created ES knowledge index: {}", indexName());
    }

    public long countDocuments() throws IOException {
        if (!indexExists()) {
            return 0;
        }
        SearchSourceBuilder ssb = new SearchSourceBuilder();
        ssb.query(QueryBuilders.matchAllQuery());
        ssb.size(0);
        ssb.trackTotalHits(true);
        SearchRequest sr = new SearchRequest(indexName());
        sr.source(ssb);
        SearchResponse resp = esClient.search(sr, RequestOptions.DEFAULT);
        return resp.getHits().getTotalHits().value;
    }

    public void bulkIndex(List<KnowledgeChunkDoc> chunks) throws IOException {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        BulkRequest bulk = new BulkRequest();
        for (KnowledgeChunkDoc c : chunks) {
            Map<String, Object> src = new HashMap<>();
            src.put("chunk_id", c.chunkId());
            src.put("source_file", c.sourceFile());
            src.put("text", c.text());
            src.put("embedding", c.embedding());
            IndexRequest ir = new IndexRequest(indexName()).id(escapeDocId(c.chunkId())).source(src);
            bulk.add(ir);
        }
        esClient.bulk(bulk, RequestOptions.DEFAULT);
    }

    public void deleteIndexIfExists() throws IOException {
        if (!indexExists()) {
            return;
        }
        AcknowledgedResponse del = esClient.indices().delete(new DeleteIndexRequest(indexName()), RequestOptions.DEFAULT);
        log.info("deleted ES index {} acknowledged={}", indexName(), del.isAcknowledged());
    }

    /**
     * 余弦相似度排序（+1 使分为正）。ES 7.12 dense_vector + script_score。
     */
    public List<KnowledgeHit> searchByVector(float[] queryVector, int topK) throws IOException {
        List<Float> qv = new ArrayList<>(queryVector.length);
        for (float v : queryVector) {
            qv.add(v);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("qv", qv);
        String source = "cosineSimilarity(params.qv, 'embedding') + 1.0";
        Script script = new Script(ScriptType.INLINE, "painless", source, params);

        SearchSourceBuilder ssb = new SearchSourceBuilder();
        ssb.query(QueryBuilders.scriptScoreQuery(QueryBuilders.matchAllQuery(), script));
        ssb.size(Math.max(1, topK));

        SearchRequest sr = new SearchRequest(indexName());
        sr.source(ssb);
        SearchResponse resp = esClient.search(sr, RequestOptions.DEFAULT);
        List<KnowledgeHit> hits = new ArrayList<>();
        for (SearchHit sh : resp.getHits().getHits()) {
            Map<String, Object> m = sh.getSourceAsMap();
            Object t = m.get("text");
            Object cid = m.get("chunk_id");
            Object sf = m.get("source_file");
            hits.add(new KnowledgeHit(
                    cid != null ? String.valueOf(cid) : sh.getId(),
                    sf != null ? String.valueOf(sf) : "",
                    t != null ? String.valueOf(t) : "",
                    sh.getScore()));
        }
        return hits;
    }

    private static String escapeDocId(String chunkId) {
        // ES _id 避免过长特殊字符
        return chunkId.replace("/", "_");
    }

    public static final class KnowledgeChunkDoc {
        private final String chunkId;
        private final String sourceFile;
        private final String text;
        private final float[] embedding;

        public KnowledgeChunkDoc(String chunkId, String sourceFile, String text, float[] embedding) {
            this.chunkId = chunkId;
            this.sourceFile = sourceFile;
            this.text = text;
            this.embedding = embedding;
        }

        public String chunkId() {
            return chunkId;
        }

        public String sourceFile() {
            return sourceFile;
        }

        public String text() {
            return text;
        }

        public float[] embedding() {
            return embedding;
        }
    }

    public static final class KnowledgeHit {
        private final String chunkId;
        private final String sourceFile;
        private final String text;
        private final float score;

        public KnowledgeHit(String chunkId, String sourceFile, String text, float score) {
            this.chunkId = chunkId;
            this.sourceFile = sourceFile;
            this.text = text;
            this.score = score;
        }

        public String chunkId() {
            return chunkId;
        }

        public String sourceFile() {
            return sourceFile;
        }

        public String text() {
            return text;
        }

        public float score() {
            return score;
        }
    }
}
