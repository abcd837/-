package com.smartprocurement.module.role.dto;

import com.smartprocurement.module.role.entity.SysRole;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer status,
        LocalDateTime createdAt
) {
    public static RoleResponse from(SysRole role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getStatus(),
                role.getCreatedAt()
        );
    }
}
