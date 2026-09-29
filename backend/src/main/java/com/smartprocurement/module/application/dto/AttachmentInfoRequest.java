package com.smartprocurement.module.application.dto;

public record AttachmentInfoRequest(
                Long id,
                String fileName,
                String storedName,
                String filePath,
                long fileSize,
                String contentType) {
}
