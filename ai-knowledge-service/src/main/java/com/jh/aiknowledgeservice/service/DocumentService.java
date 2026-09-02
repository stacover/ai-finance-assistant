package com.jh.aiknowledgeservice.service;

import com.jh.aiknowledgeservice.dto.DocumentChunkResponse;
import com.jh.aiknowledgeservice.dto.DocumentParseResponse;
import com.jh.aiknowledgeservice.dto.DocumentSearchResult;
import com.jh.aiknowledgeservice.transformer.DocumentTextCleaner;
import com.jh.aiknowledgeservice.transformer.ShortDocumentMerger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author jinhang
 * @since 2026/8/26 21:55
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
  private final DocumentTextCleaner documentTextCleaner;
  private final ShortDocumentMerger shortDocumentMerger;
  private final EmbeddingModel embeddingModel;
  private final EmbeddingService embeddingService;

  public List<DocumentParseResponse> parseDocument(MultipartFile file) {
    TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
    List<Document> documents = reader.read();
    return documents.stream()
        .map(
            document ->
                new DocumentParseResponse(
                    document.getId(),
                    file.getOriginalFilename(),
                    document.getText() == null ? 0 : document.getText().length(),
                    document.getText(),
                    document.getMetadata()))
        .toList();
  }

  /**
   * 按页读取 PDF。优先使用 Spring AI 的 PagePdfDocumentReader（可还原布局）； 当遇到坐标异常的 PDF（如文本元素坐标为 NaN）触发
   * "Comparison method violates its general contract" 时，降级使用 PDFBox 原生
   * PDFTextStripper（关闭按位置排序）逐页提取，保证解析不中断。
   */
  private List<Document> readPdfByPage(MultipartFile file) {
    try {
      PagePdfDocumentReader reader = new PagePdfDocumentReader(file.getResource());
      return reader.read();
    } catch (IllegalArgumentException ex) {
      log.warn(
          "PagePdfDocumentReader failed on file {} ({}), falling back to PDFTextStripper with sort disabled",
          file.getOriginalFilename(),
          ex.getMessage());
      return readPdfByPageFallback(file);
    }
  }

  private List<Document> readPdfByPageFallback(MultipartFile file) {
    List<Document> documents = new ArrayList<>();
    try (PDDocument pdDocument = Loader.loadPDF(file.getBytes())) {
      int pageCount = pdDocument.getNumberOfPages();
      for (int i = 0; i < pageCount; i++) {
        PDFTextStripper stripper = new PDFTextStripper();
        // 关闭按位置排序，避免 TextPositionComparator 对 NaN 坐标触发比较器契约异常
        stripper.setSortByPosition(false);
        stripper.setStartPage(i + 1);
        stripper.setEndPage(i + 1);
        String text = stripper.getText(pdDocument);
        if (text == null || text.isBlank()) {
          continue;
        }
        Document document = new Document(text, Map.of("pageNumber", i + 1));
        documents.add(document);
      }
    } catch (Exception ex) {
      log.error("PDF fallback parsing failed for file {}", file.getOriginalFilename(), ex);
      throw new IllegalStateException("PDF 解析失败: " + ex.getMessage(), ex);
    }
    return documents;
  }

  public List<DocumentChunkResponse> chunks(MultipartFile file) {
    List<Document> documents = readPdfByPage(file);
    documents = documentTextCleaner.clean(documents);
    TokenTextSplitter splitter =
        TokenTextSplitter.builder()
            .withChunkSize(800)
            .withMinChunkSizeChars(350)
            .withMinChunkLengthToEmbed(10)
            .withMaxNumChunks(5000)
            .withKeepSeparator(true)
            .build();

    List<Document> chunks = splitter.apply(documents);

    List<Document> mergedChunks = shortDocumentMerger.merge(chunks);
    return IntStream.range(0, mergedChunks.size())
        .mapToObj(
            i -> {
              Document chunk = mergedChunks.get(i);
              chunk.getMetadata().put("chunkIndex", i);
              return new DocumentChunkResponse(
                  i,
                  chunk.getId(),
                  chunk.getText() == null ? 0 : chunk.getText().length(),
                  chunk.getText(),
                  chunk.getMetadata());
            })
        .toList();
  }

  public List<DocumentSearchResult> search(MultipartFile file, String query, int topK) {
    List<DocumentChunkResponse> chunks = chunks(file);

    float[] queryVector = embeddingModel.embed(query);

    return chunks.stream()
        .map(
            chunk -> {
              float[] chunkVector = embeddingModel.embed(chunk.text());
              double similarity = embeddingService.cosineSimilarity(chunkVector, queryVector);
              return new DocumentSearchResult(
                  (Integer) chunk.metadata().get("chunkIndex"),
                  (Integer) chunk.metadata().get("page_number"),
                  chunk.text(),
                  similarity);
            })
        .sorted(Comparator.comparing(DocumentSearchResult::similarity).reversed())
        .limit(topK)
        .toList();
  }
}
