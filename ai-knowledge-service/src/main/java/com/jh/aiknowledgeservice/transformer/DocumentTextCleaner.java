package com.jh.aiknowledgeservice.transformer;

import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * @author jinhang
 * @since 2026/8/31 21:35
 */
@Component
public class DocumentTextCleaner {
  public List<Document> clean(List<Document> documents) {
    return documents.stream()
        .map(this::cleanDocument)
        .filter(document -> document.getText() != null && !document.getText().isEmpty())
        .toList();
  }

  private Document cleanDocument(Document document) {
    String text = document.getText();
    if (StringUtils.isBlank(text)) {
      return document;
    }
    String cleanedText = // 统一换行
        text.replace("\r\n", "\n")
            .replace("\r", "\n")
            // Tab / 连续横向空白压缩
            .replaceAll("[\\t ]+", " ")
            // 去掉每行开头结尾空格
            .replaceAll("(?m)^ +| +$", "")
            // 连续3个以上换行压成2个
            .replaceAll("\\n{3,}", "\n\n")
            // 去掉测试文档的固定页眉
            .replaceAll("(?m)^RAG 测试文档\\s*-\\s*Java AI 应用开发\\s*$", "")
            // 去掉页码
            .replaceAll("(?m)^第\\s*\\d+\\s*页\\s*$", "")
            .replace("\uFFFD", "")
            .trim();
    return new Document(document.getId(), cleanedText, new HashMap<>(document.getMetadata()));
  }
}
