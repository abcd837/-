package com.smartprocurement.module.user.dto;

import com.smartprocurement.module.user.entity.SysUser;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String userNo,
        String username,
        String displayName,
        Long departmentId,
        String email,
        String mobile,
        Integer status,
        LocalDateTime createdAt,
        List<Long> roleIds
) {
    public static UserResponse from(SysUser user) {
        return new UserResponse(
                user.getId(),
                user.getUserNo(),
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartmentId(),
                user.getEmail(),
                user.getMobile(),
                user.getStatus(),
                user.getCreatedAt(),
                null
        );
    }

    public UserResponse withRoleIds(List<Long> roleIds) {
        return new UserResponse(
                id(), userNo(), username(), displayName(), departmentId(),
                email(), mobile(), status(), createdAt(), roleIds
        );
    }
}
