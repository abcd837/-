package com.smartprocurement.module.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ApplicationCreateRequest(
        @NotBlank(message = "申请标题不能为空")
        String title,

        @NotNull(message = "申请部门不能为空")
        Long departmentId,

        @NotNull(message = "审批人不能为空")
        Long approverId,

        String applicationType,
        String purpose,
        String budgetAccountCode,
        String budgetAccountName,
        BigDecimal budgetAvailableAmount,
        String currency,
        LocalDate requiredDate,

        @Valid
        @NotEmpty(message = "采购明细不能为空")
        List<ApplicationItemRequest> items,

        @Valid
        List<AttachmentInfoRequest> attachments
) {
}
