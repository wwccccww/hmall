package com.hmall.ai.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 对用户问题做 embedding 后在 ES 中取 Top-K 知识片段。
 */
@Slf4j
@Service
@ConditionalOnBean(KnowledgeEsIndexService.class)
@RequiredArgsConstructor
public class KnowledgeVectorSearchService {

    private final KnowledgeEsIndexService esIndexService;
    private final EmbeddingClient embeddingClient;
    private final KnowledgeProperties knowledgeProperties;

    public List<KnowledgeEsIndexService.KnowledgeHit> search(String userMessage) throws IOException {
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }
        List<float[]> vecs = embeddingClient.embedBatch(List.of(userMessage.trim()));
        if (vecs.isEmpty()) {
            return List.of();
        }
        return esIndexService.searchByVector(vecs.get(0), knowledgeProperties.getSearchTopK());
    }
}
