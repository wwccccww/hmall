package com.hmall.ai.web;

import com.hmall.ai.knowledge.KnowledgeIndexerService;
import com.hmall.ai.knowledge.KnowledgeProperties;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Api(tags = "AI 知识库管理")
@RestController
@RequestMapping("/ai/admin")
@ConditionalOnBean(KnowledgeIndexerService.class)
public class KnowledgeAdminController {

    private final KnowledgeIndexerService knowledgeIndexerService;
    private final KnowledgeProperties knowledgeProperties;

    public KnowledgeAdminController(
            KnowledgeIndexerService knowledgeIndexerService,
            KnowledgeProperties knowledgeProperties) {
        this.knowledgeIndexerService = knowledgeIndexerService;
        this.knowledgeProperties = knowledgeProperties;
    }

    @ApiOperation("全量重建向量知识库索引（需 ES + Embedding）")
    @PostMapping("/reindex-kb")
    public ResponseEntity<Map<String, Object>> reindexKb(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        String expect = knowledgeProperties.getAdminToken();
        if (expect != null && !expect.isBlank()) {
            if (token == null || !expect.equals(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("ok", false, "error", "Forbidden: X-Admin-Token"));
            }
        }
        try {
            int n = knowledgeIndexerService.reindexAll();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("chunks", n);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "error", String.valueOf(e.getMessage())));
        }
    }
}
