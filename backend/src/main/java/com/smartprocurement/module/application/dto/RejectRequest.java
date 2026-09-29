package com.smartprocurement.module.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 审批驳回请求，驳回理由必填 */
public record RejectRequest(
        @NotBlank(message = "驳回理由不能为空")
        String comment
) {
}
