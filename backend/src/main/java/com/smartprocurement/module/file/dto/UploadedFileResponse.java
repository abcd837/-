package com.smartprocurement.module.file.dto;

public record UploadedFileResponse(
        String fileName,
        String storedName,
        String filePath,
        long fileSize,
        String contentType
) {
}
