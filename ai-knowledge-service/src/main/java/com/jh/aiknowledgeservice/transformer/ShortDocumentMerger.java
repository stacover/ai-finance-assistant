package com.jh.aiknowledgeservice.transformer;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jinhang
 * @since 2026/8/31 21:51
 */

@Component
public class ShortDocumentMerger {
    private static final int MIN_CHUNK_LENGTH = 120;

    public List<Document> merge(List<Document> documents) {

        List<Document> result = new ArrayList<>();

        for (Document current : documents) {

            String currentText = current.getText();

            if (currentText == null || currentText.isBlank()) {
                continue;
            }

            if (currentText.length() >= MIN_CHUNK_LENGTH) {
                result.add(current);
                continue;
            }

            if (result.isEmpty()) {
                result.add(current);
                continue;
            }

            Document previous =
                    result.get(result.size() - 1);

            if (samePage(previous, current)) {

                Document merged =
                        mergeDocument(previous, current);

                result.set(
                        result.size() - 1,
                        merged
                );

            } else {

                result.add(current);
            }
        }

        return result;
    }

    private boolean samePage(
            Document previous,
            Document current
    ) {

        Object previousPage =
                previous.getMetadata()
                        .get("page_number");

        Object currentPage =
                current.getMetadata()
                        .get("page_number");

        return previousPage != null
                && previousPage.equals(currentPage);
    }

    private Document mergeDocument(
            Document previous,
            Document current
    ) {

        String mergedText =
                previous.getText()
                        + "\n"
                        + current.getText();

        Map<String, Object> metadata =
                new HashMap<>(
                        previous.getMetadata()
                );

        return new Document(
                previous.getId(),
                mergedText,
                metadata
        );
    }
}
