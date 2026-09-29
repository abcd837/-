package com.smartprocurement.module.department.controller;

import com.smartprocurement.common.api.ApiResponse;
import com.smartprocurement.config.RequireRole;
import com.smartprocurement.module.department.dto.DepartmentCreateRequest;
import com.smartprocurement.module.department.dto.DepartmentResponse;
import com.smartprocurement.module.department.dto.DepartmentUpdateRequest;
import com.smartprocurement.module.department.service.DepartmentService;
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
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /** 部门列表放开给所有登录用户（采购申请创建时需要下拉选择） */
    @GetMapping
    public ApiResponse<List<DepartmentResponse>> list() {
        return ApiResponse.ok(departmentService.list());
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public ApiResponse<DepartmentResponse> create(
            @Valid @RequestBody DepartmentCreateRequest request
    ) {
        return ApiResponse.ok(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN"})
    public ApiResponse<DepartmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest request
    ) {
        return ApiResponse.ok(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.ok();
    }
}
