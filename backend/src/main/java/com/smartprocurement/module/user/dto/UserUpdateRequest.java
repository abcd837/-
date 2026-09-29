package com.smartprocurement.module.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UserUpdateRequest(
        @NotBlank(message = "用户编号不能为空")
        String userNo,

        @NotBlank(message = "登录账号不能为空")
        String username,

        @NotBlank(message = "姓名不能为空")
        String displayName,

        Long departmentId,
        String email,
        String mobile,
        String password,
        Integer status,
        List<Long> roleIds
) {
}
