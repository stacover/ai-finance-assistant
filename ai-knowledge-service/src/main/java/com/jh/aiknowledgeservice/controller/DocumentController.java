package com.jh.aiknowledgeservice.controller;

import com.jh.aiknowledgeservice.dto.DocumentChunkResponse;
import com.jh.aiknowledgeservice.dto.DocumentParseResponse;
import com.jh.aiknowledgeservice.dto.DocumentSearchResult;
import com.jh.aiknowledgeservice.service.DocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author jinhang
 * @since 2026/8/26 22:02
 */

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping(value = "/parse",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<DocumentParseResponse> parseDocument(@RequestParam("file") MultipartFile file) {
        return documentService.parseDocument(file);
    }

    @PostMapping(
            value = "/chunks",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<DocumentChunkResponse> chunks(
            @RequestPart("file") MultipartFile file
    ) {
        return documentService.chunks(file);
    }

    @PostMapping(
            value = "/search",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<DocumentSearchResult> search(
            @RequestPart("file") MultipartFile file,
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK
    ) {

        return documentService.search(
                file,
                query,
                topK
        );
    }
}
