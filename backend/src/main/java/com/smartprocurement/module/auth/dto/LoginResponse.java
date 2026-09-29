package com.smartprocurement.module.auth.dto;

import java.util.List;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String displayName,
        Long departmentId,
        List<Long> roleIds,
        List<String> roleCodes
) {
}
