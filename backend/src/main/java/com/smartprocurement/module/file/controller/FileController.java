package com.smartprocurement.module.file.controller;

import com.smartprocurement.common.api.ApiResponse;
import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.file.dto.UploadedFileResponse;
import com.smartprocurement.module.file.entity.ProcurementAttachment;
import com.smartprocurement.module.file.mapper.ProcurementAttachmentMapper;
import com.smartprocurement.module.file.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
public class FileController {

        private final FileStorageService fileStorageService;
        private final ProcurementAttachmentMapper attachmentMapper;

        public FileController(FileStorageService fileStorageService,
                        ProcurementAttachmentMapper attachmentMapper) {
                this.fileStorageService = fileStorageService;
                this.attachmentMapper = attachmentMapper;
        }

        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ApiResponse<UploadedFileResponse> upload(
                        @RequestParam("file") MultipartFile file) {
                return ApiResponse.ok(fileStorageService.store(file));
        }

        /**
         * 下载或预览附件。
         * disposition=inline 时浏览器内预览（图片/PDF），attachment 时触发下载。
         */
        @GetMapping("/{id}")
        public ResponseEntity<byte[]> fetch(
                        @PathVariable Long id,
                        @RequestParam(defaultValue = "attachment") String disposition) {
                ProcurementAttachment attachment = attachmentMapper.selectById(id);
                if (attachment == null) {
                        throw new BusinessException("附件不存在");
                }
                try {
                        byte[] content = fileStorageService.loadFileBytes(attachment);
                        String encodedName = URLEncoder.encode(
                                        attachment.getFileName() == null ? "file" : attachment.getFileName(),
                                        StandardCharsets.UTF_8).replace("+", "%20");

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.parseMediaType(
                                        attachment.getContentType() != null ? attachment.getContentType()
                                                        : MediaType.APPLICATION_OCTET_STREAM_VALUE));
                        headers.setContentDispositionFormData("attachment", encodedName);
                        // inline 预览时覆盖为 inline
                        if ("inline".equalsIgnoreCase(disposition)) {
                                headers.set(HttpHeaders.CONTENT_DISPOSITION,
                                                "inline; filename*=UTF-8''" + encodedName);
                        } else {
                                headers.set(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename*=UTF-8''" + encodedName);
                        }
                        return ResponseEntity.ok()
                                        .headers(headers)
                                        .body(content);
                } catch (IOException e) {
                        throw new BusinessException("附件读取失败");
                }
        }
}
