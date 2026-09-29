package com.smartprocurement.module.department.dto;

import com.smartprocurement.module.department.entity.SysDepartment;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        Long parentId,
        Integer sortOrder
) {
    public static DepartmentResponse from(SysDepartment department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getParentId(),
                department.getSortOrder()
        );
    }
}
