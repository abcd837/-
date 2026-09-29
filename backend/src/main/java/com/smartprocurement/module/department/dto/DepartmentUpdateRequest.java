package com.smartprocurement.module.department.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentUpdateRequest(
        @NotBlank(message = "部门编码不能为空")
        String code,

        @NotBlank(message = "部门名称不能为空")
        String name,

        Long parentId,
        Integer sortOrder
) {
}
