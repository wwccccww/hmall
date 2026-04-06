package com.hmall.ai.knowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 将 knowledge-base 下 Markdown 按二级标题 {@code ## } 切成片段（含「问：/答：」）。
 */
public final class KnowledgeMarkdownChunker {

    private static final Pattern H2_SPLIT = Pattern.compile("(?m)^##\\s+");

    private KnowledgeMarkdownChunker() {
    }

    public static final class TextChunk {
        private final String sourceFile;
        private final String chunkId;
        private final String text;

        public TextChunk(String sourceFile, String chunkId, String text) {
            this.sourceFile = sourceFile;
            this.chunkId = chunkId;
            this.text = text;
        }

        public String sourceFile() {
            return sourceFile;
        }

        public String chunkId() {
            return chunkId;
        }

        public String text() {
            return text;
        }
    }

    /**
     * @param sourceFile 文件名，用于 chunk_id 与 source_file 字段
     * @param content    全文
     */
    public static List<TextChunk> chunk(String sourceFile, String content) {
        List<TextChunk> out = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return out;
        }
        String base = sourceFile;
        int dot = sourceFile.lastIndexOf('.');
        if (dot > 0) {
            base = sourceFile.substring(0, dot);
        }
        String[] parts = H2_SPLIT.split(content);
        if (parts.length <= 1) {
            out.add(new TextChunk(sourceFile, base + "#0", content.trim()));
            return out;
        }
        int serial = 0;
        String head = parts[0].trim();
        if (!head.isBlank()) {
            out.add(new TextChunk(sourceFile, base + "#intro", head));
        }
        for (int i = 1; i < parts.length; i++) {
            String block = "## " + parts[i].trim();
            if (block.length() <= 3) {
                continue;
            }
            serial++;
            out.add(new TextChunk(sourceFile, base + "#q" + serial, block));
        }
        return out;
    }
}
