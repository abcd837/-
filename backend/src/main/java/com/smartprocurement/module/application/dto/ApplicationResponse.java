package com.smartprocurement.module.application.dto;

import com.smartprocurement.module.application.entity.ProcurementApplication;
import com.smartprocurement.module.application.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        String applicationNo,
        String title,
        Long applicantId,
        String applicantName,
        Long approverId,
        String approverName,
        Long departmentId,
        String departmentName,
        String applicationType,
        String purpose,
        BigDecimal totalAmount,
        String currency,
        LocalDate requiredDate,
        ApplicationStatus status,
        String approvalComment,
        LocalDateTime submittedAt,
        LocalDateTime createdAt) {
    public static ApplicationResponse from(
            ProcurementApplication application,
            String applicantName,
            String approverName,
            String departmentName) {
        return new ApplicationResponse(
                application.getId(),
                application.getApplicationNo(),
                application.getTitle(),
                application.getApplicantId(),
                applicantName,
                application.getApproverId(),
                approverName,
                application.getDepartmentId(),
                departmentName,
                application.getApplicationType(),
                application.getPurpose(),
                application.getTotalAmount(),
                application.getCurrency(),
                application.getRequiredDate(),
                application.getStatus(),
                application.getApprovalComment(),
                application.getSubmittedAt(),
                application.getCreatedAt());
    }

    public static ApplicationResponse from(ProcurementApplication application) {
        return from(application, null, null, null);
    }

    /** XML 联表查询行 → 对外响应，状态字符串在这里转枚举，非法值兜底为 null */
    public static ApplicationResponse fromRow(ApplicationListRow row) {
        ApplicationStatus statusEnum = null;
        if (row.getStatus() != null) {
            try {
                statusEnum = ApplicationStatus.valueOf(row.getStatus());
            } catch (IllegalArgumentException ignored) {
                // 数据库出现未知状态码时不炸接口，前端按原值展示
            }
        }
        return new ApplicationResponse(
                row.getId(),
                row.getApplicationNo(),
                row.getTitle(),
                row.getApplicantId(),
                row.getApplicantName(),
                row.getApproverId(),
                row.getApproverName(),
                row.getDepartmentId(),
                row.getDepartmentName(),
                row.getApplicationType(),
                row.getPurpose(),
                row.getTotalAmount(),
                row.getCurrency(),
                row.getRequiredDate(),
                statusEnum,
                row.getApprovalComment(),
                row.getSubmittedAt(),
                row.getCreatedAt());
    }
}
