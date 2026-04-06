package com.hmall.ai.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 启动时若索引为空则异步全量导入（可关 {@link KnowledgeProperties#isAutoReindexIfEmpty()}）。
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE)
@ConditionalOnBean(KnowledgeIndexerService.class)
@RequiredArgsConstructor
public class KnowledgeIndexBootstrap implements ApplicationRunner {

    private final KnowledgeProperties knowledgeProperties;
    private final KnowledgeIndexerService knowledgeIndexerService;
    private final KnowledgeEsIndexService knowledgeEsIndexService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            knowledgeEsIndexService.createIndexIfNotExists();
        } catch (Exception e) {
            log.warn(
                    "知识库 ES 创建索引失败：已尝试 http://{}:{} ，原因: {}。"
                            + " 若在本机 Windows 跑服务、ES 在虚拟机 Docker，请设置环境变量 HM_AI_KNOWLEDGE_ES_HOST 为虚拟机网卡 IP，并放行 9200；"
                            + " 若在虚拟机宿主机跑服务一般可用 127.0.0.1。",
                    knowledgeProperties.getEsHost(),
                    knowledgeProperties.getEsPort(),
                    e.getMessage());
            return;
        }
        if (!knowledgeProperties.isAutoReindexIfEmpty()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                long n = knowledgeEsIndexService.countDocuments();
                if (n == 0) {
                    log.info("知识库 ES 索引为空，开始自动全量导入（Embedding + bulk）…");
                    knowledgeIndexerService.reindexAll();
                } else {
                    log.debug("知识库 ES 已有 {} 条文档，跳过自动导入", n);
                }
            } catch (Exception e) {
                log.warn("知识库自动导入未执行（可检查 Embedding 或调用 POST /ai/admin/reindex-kb）: {}", e.getMessage());
            }
        });
    }
}
