package com.smartprocurement.module.supplier.controller;

import com.smartprocurement.common.api.ApiResponse;
import com.smartprocurement.config.AuthInterceptor;
import com.smartprocurement.config.RequireRole;
import com.smartprocurement.module.auth.dto.LoginResponse;
import com.smartprocurement.module.supplier.dto.SupplierResponse;
import com.smartprocurement.module.supplier.dto.SupplierSaveRequest;
import com.smartprocurement.module.supplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /** 供应商档案列表：采购员和管理员可查看 */
    @GetMapping
    @RequireRole({ "BUYER", "ADMIN" })
    public ApiResponse<List<SupplierResponse>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(supplierService.list(keyword));
    }

    /** 供应商档案由采购员和管理员维护 */
    @PostMapping
    @RequireRole({ "BUYER", "ADMIN" })
    public ApiResponse<SupplierResponse> create(
            @Valid @RequestBody SupplierSaveRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        return ApiResponse.ok(supplierService.create(request, currentUser.userId()));
    }

    @PutMapping("/{id}")
    @RequireRole({ "BUYER", "ADMIN" })
    public ApiResponse<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierSaveRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        return ApiResponse.ok(supplierService.update(id, request, currentUser.userId()));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ "BUYER", "ADMIN" })
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.ok();
    }
}
