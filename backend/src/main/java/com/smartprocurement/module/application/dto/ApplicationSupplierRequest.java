package com.smartprocurement.module.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** 采购申请候选供应商录入请求（手动输入） */
public record ApplicationSupplierRequest(

        @NotBlank(message = "供应商名称不能为空")
        String supplierName,

        String contactPerson,
        String contactPhone,
        BigDecimal quotedAmount,
        Integer deliveryDays,
        String remark
) {
}
