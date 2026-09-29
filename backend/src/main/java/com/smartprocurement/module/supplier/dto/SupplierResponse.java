package com.smartprocurement.module.supplier.dto;

import com.smartprocurement.module.supplier.entity.Supplier;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        String supplierNo,
        String name,
        String shortName,
        String contactPerson,
        String contactPhone,
        String email,
        String address,
        String remark,
        Long applicationId,
        String applicationNo,
        Integer status,
        LocalDateTime createdAt
) {
    public static SupplierResponse from(Supplier s, String applicationNo) {
        return new SupplierResponse(
                s.getId(),
                s.getSupplierNo(),
                s.getName(),
                s.getShortName(),
                s.getContactPerson(),
                s.getContactPhone(),
                s.getEmail(),
                s.getAddress(),
                s.getRemark(),
                s.getApplicationId(),
                applicationNo,
                s.getStatus(),
                s.getCreatedAt()
        );
    }
}
