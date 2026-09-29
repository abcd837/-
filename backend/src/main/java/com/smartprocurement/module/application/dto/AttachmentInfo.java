package com.smartprocurement.module.application.dto;

import com.smartprocurement.module.file.entity.ProcurementAttachment;

import java.time.LocalDateTime;

/** 附件响应信息，用于申请详情回显 */
public record AttachmentInfo(
        Long id,
        String fileName,
        String storedName,
        String filePath,
        Long fileSize,
        String contentType,
        LocalDateTime createdAt) {
    public static AttachmentInfo from(ProcurementAttachment attachment) {
        return new AttachmentInfo(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getStoredName(),
                attachment.getFilePath(),
                attachment.getFileSize(),
                attachment.getContentType(),
                attachment.getCreatedAt());
    }
}
