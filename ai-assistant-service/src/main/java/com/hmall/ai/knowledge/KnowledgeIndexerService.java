package com.hmall.ai.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 classpath knowledge-base/*.md 切分、嵌入并 bulk 写入 ES。
 */
@Slf4j
@Service
@ConditionalOnBean(KnowledgeEsIndexService.class)
@RequiredArgsConstructor
public class KnowledgeIndexerService {

    private final KnowledgeEsIndexService esIndexService;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties embeddingProperties;

    /**
     * 全量重建：确保索引存在后，批量写入（以 chunk_id 为文档 id 覆盖）。
     */
    public int reindexAll() throws IOException {
        esIndexService.createIndexIfNotExists();
        List<KnowledgeMarkdownChunker.TextChunk> allChunks = loadClasspathChunks();
        if (allChunks.isEmpty()) {
            log.warn("no markdown chunks found under classpath:knowledge-base/");
            return 0;
        }
        int batch = Math.max(1, embeddingProperties.getBatchSize());
        int totalIndexed = 0;
        for (int i = 0; i < allChunks.size(); i += batch) {
            int end = Math.min(i + batch, allChunks.size());
            List<KnowledgeMarkdownChunker.TextChunk> slice = allChunks.subList(i, end);
            List<String> texts = new ArrayList<>();
            for (KnowledgeMarkdownChunker.TextChunk tc : slice) {
                texts.add(tc.text());
            }
            List<float[]> vectors = embeddingClient.embedBatch(texts);
            if (vectors.size() != slice.size()) {
                throw new IllegalStateException("embedding batch size mismatch");
            }
            List<KnowledgeEsIndexService.KnowledgeChunkDoc> docs = new ArrayList<>();
            for (int j = 0; j < slice.size(); j++) {
                KnowledgeMarkdownChunker.TextChunk tc = slice.get(j);
                docs.add(new KnowledgeEsIndexService.KnowledgeChunkDoc(tc.chunkId(), tc.sourceFile(), tc.text(), vectors.get(j)));
            }
            esIndexService.bulkIndex(docs);
            totalIndexed += docs.size();
        }
        log.info("knowledge reindex done, chunks={}", totalIndexed);
        return totalIndexed;
    }

    private List<KnowledgeMarkdownChunker.TextChunk> loadClasspathChunks() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:knowledge-base/*.md");
        List<KnowledgeMarkdownChunker.TextChunk> out = new ArrayList<>();
        for (Resource res : resources) {
            String filename = res.getFilename();
            if (filename == null) {
                continue;
            }
            String content = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
            List<KnowledgeMarkdownChunker.TextChunk> fileChunks = KnowledgeMarkdownChunker.chunk(filename, content);
            out.addAll(fileChunks);
        }
        return out;
    }
}
