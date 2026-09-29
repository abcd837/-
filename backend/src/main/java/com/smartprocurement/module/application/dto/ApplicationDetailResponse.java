package com.smartprocurement.module.application.dto;

import com.smartprocurement.module.application.entity.ProcurementApplication;
import com.smartprocurement.module.application.entity.ProcurementApplicationItem;
import com.smartprocurement.module.file.entity.ProcurementAttachment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ApplicationDetailResponse(
                Long id,
                String applicationNo,
                String title,
                Long applicantId,
                Long approverId,
                Long departmentId,
                String applicationType,
                String purpose,
                BigDecimal totalAmount,
                String currency,
                LocalDate requiredDate,
                String status,
                String approvalComment,
                LocalDateTime submittedAt,
                LocalDateTime createdAt,
                List<ApplicationItemResponse> items,
                List<AttachmentInfo> attachments) {
        public static ApplicationDetailResponse from(
                        ProcurementApplication application,
                        List<ProcurementApplicationItem> items,
                        List<ProcurementAttachment> attachments) {
                return new ApplicationDetailResponse(
                                application.getId(),
                                application.getApplicationNo(),
                                application.getTitle(),
                                application.getApplicantId(),
                                application.getApproverId(),
                                application.getDepartmentId(),
                                application.getApplicationType(),
                                application.getPurpose(),
                                application.getTotalAmount(),
                                application.getCurrency(),
                                application.getRequiredDate(),
                                application.getStatus() == null ? null : application.getStatus().name(),
                                application.getApprovalComment(),
                                application.getSubmittedAt(),
                                application.getCreatedAt(),
                                items.stream().map(ApplicationItemResponse::from).toList(),
                                attachments.stream().map(AttachmentInfo::from).toList());
        }
}
