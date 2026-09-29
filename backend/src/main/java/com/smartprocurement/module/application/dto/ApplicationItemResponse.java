package com.smartprocurement.module.application.dto;

import com.smartprocurement.module.application.entity.ProcurementApplicationItem;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationItemResponse(
        Long id,
        Integer lineNo,
        String materialCode,
        String materialName,
        String specification,
        String unit,
        BigDecimal quantity,
        BigDecimal estimatedUnitPrice,
        BigDecimal estimatedAmount,
        LocalDate requiredDate,
        String suggestSupplierCode,
        String suggestSupplierName,
        String remark
) {
    public static ApplicationItemResponse from(ProcurementApplicationItem item) {
        return new ApplicationItemResponse(
                item.getId(),
                item.getLineNo(),
                item.getMaterialCode(),
                item.getMaterialName(),
                item.getSpecification(),
                item.getUnit(),
                item.getQuantity(),
                item.getEstimatedUnitPrice(),
                item.getEstimatedAmount(),
                item.getRequiredDate(),
                item.getSuggestSupplierCode(),
                item.getSuggestSupplierName(),
                item.getRemark()
        );
    }
}
