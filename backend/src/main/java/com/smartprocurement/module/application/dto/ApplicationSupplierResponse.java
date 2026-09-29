package com.smartprocurement.module.application.dto;

import com.smartprocurement.module.application.entity.ProcurementApplicationSupplier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApplicationSupplierResponse(
        Long id,
        Long applicationId,
        String supplierName,
        String contactPerson,
        String contactPhone,
        BigDecimal quotedAmount,
        Integer deliveryDays,
        Boolean selected,
        String remark,
        LocalDateTime createdAt
) {
    public static ApplicationSupplierResponse from(ProcurementApplicationSupplier s) {
        return new ApplicationSupplierResponse(
                s.getId(),
                s.getApplicationId(),
                s.getSupplierName(),
                s.getContactPerson(),
                s.getContactPhone(),
                s.getQuotedAmount(),
                s.getDeliveryDays(),
                s.getIsSelected() != null && s.getIsSelected() == 1,
                s.getRemark(),
                s.getCreatedAt()
        );
    }
}
