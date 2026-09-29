package com.smartprocurement.module.application.dto;

/** 审批通过请求，审批意见可选 */
public record ApproveRequest(
        String comment
) {
}
