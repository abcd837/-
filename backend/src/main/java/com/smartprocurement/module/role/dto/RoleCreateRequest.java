package com.smartprocurement.module.role.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleCreateRequest(
        @NotBlank(message = "角色编码不能为空")
        String code,

        @NotBlank(message = "角色名称不能为空")
        String name,

        String description
) {
}
