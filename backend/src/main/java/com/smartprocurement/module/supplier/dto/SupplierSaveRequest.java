package com.smartprocurement.module.supplier.dto;

import jakarta.validation.constraints.NotBlank;

/** 供应商新增/编辑共用请求体 */
public record SupplierSaveRequest(

        @NotBlank(message = "供应商编码不能为空")
        String supplierNo,

        @NotBlank(message = "供应商名称不能为空")
        String name,

        String shortName,
        String contactPerson,
        String contactPhone,
        String email,
        String address,
        String remark,
        Long applicationId,
        Integer status
) {
}
