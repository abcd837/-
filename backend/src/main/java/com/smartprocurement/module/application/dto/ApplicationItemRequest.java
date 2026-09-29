package com.smartprocurement.module.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationItemRequest(
        @NotBlank(message = "物料名称不能为空")
        String materialName,

        String materialCode,
        String specification,
        String unit,

        @NotNull(message = "数量不能为空")
        @DecimalMin(value = "0.0001", message = "数量必须大于0")
        BigDecimal quantity,

        BigDecimal estimatedUnitPrice,
        BigDecimal estimatedAmount,
        LocalDate requiredDate,
        String suggestSupplierCode,
        String suggestSupplierName,
        String remark
) {
}
