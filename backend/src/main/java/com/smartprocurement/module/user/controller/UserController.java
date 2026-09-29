package com.smartprocurement.module.user.controller;

import com.smartprocurement.common.api.ApiResponse;
import com.smartprocurement.config.RequireRole;
import com.smartprocurement.module.user.dto.UserCreateRequest;
import com.smartprocurement.module.user.dto.UserResponse;
import com.smartprocurement.module.user.dto.UserUpdateRequest;
import com.smartprocurement.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 审批人候选列表（所有 APPROVER 或 ADMIN 角色的用户），放开给所有登录用户 */
    @GetMapping("/approvers")
    public ApiResponse<List<UserResponse>> listApprovers() {
        return ApiResponse.ok(userService.listApprovers());
    }

    @GetMapping
    @RequireRole({"ADMIN"})
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.ok(userService.list());
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN"})
    public ApiResponse<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }
}
