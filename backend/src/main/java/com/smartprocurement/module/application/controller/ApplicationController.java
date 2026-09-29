package com.smartprocurement.module.application.controller;

import com.smartprocurement.common.api.ApiResponse;
import com.smartprocurement.common.api.PageResult;
import com.smartprocurement.config.AuthInterceptor;
import com.smartprocurement.config.RequireRole;
import com.smartprocurement.module.application.enums.ApplicationStatus;
import com.smartprocurement.module.auth.dto.LoginResponse;
import com.smartprocurement.module.application.dto.ApplicationCreateRequest;
import com.smartprocurement.module.application.dto.ApplicationDetailResponse;
import com.smartprocurement.module.application.dto.ApplicationResponse;
import com.smartprocurement.module.application.dto.ApplicationSupplierRequest;
import com.smartprocurement.module.application.dto.ApplicationSupplierResponse;
import com.smartprocurement.module.application.dto.ApproveRequest;
import com.smartprocurement.module.application.dto.RejectRequest;
import com.smartprocurement.module.application.service.ApplicationExcelService;
import com.smartprocurement.module.application.service.ApplicationService;
import com.smartprocurement.module.application.service.ApplicationSupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationExcelService applicationExcelService;
    private final ApplicationSupplierService applicationSupplierService;

    public ApplicationController(ApplicationService applicationService,
            ApplicationExcelService applicationExcelService,
            ApplicationSupplierService applicationSupplierService) {
        this.applicationService = applicationService;
        this.applicationExcelService = applicationExcelService;
        this.applicationSupplierService = applicationSupplierService;
    }

    /** 构造 Excel 文件下载响应，中文文件名按 RFC 5987 编码避免乱码 */
    private ResponseEntity<byte[]> excelResponse(byte[] content, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    /** 下载采购申请导入模板（申请人/管理员） */
    @GetMapping("/import-template")
    @RequireRole({ "APPLICANT", "ADMIN" })
    public ResponseEntity<byte[]> importTemplate() {
        return excelResponse(applicationExcelService.buildTemplate(), "采购申请导入模板.xlsx");
    }

    /** 导入 Excel 生成草稿采购申请（申请人/管理员），一个文件对应一张申请 */
    @PostMapping("/import")
    @RequireRole({ "APPLICANT", "ADMIN" })
    public ApiResponse<ApplicationResponse> importApplications(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        return ApiResponse.ok(applicationExcelService.importApplications(file, currentUser.userId()));
    }

    /** 按当前筛选条件导出采购申请数据（仅管理员） */
    @GetMapping("/export")
    @RequireRole({ "ADMIN" })
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ApplicationStatus status) {
        String statusCode = status == null ? null : status.name();
        return excelResponse(applicationExcelService.exportApplications(keyword, statusCode), "采购申请数据.xlsx");
    }

    // ── 候选供应商（询价名单） ────────────────────────────────────────

    /** 查询某张申请单的候选供应商列表（采购员/采购负责人/管理员可查看） */
    @GetMapping("/{id}/suppliers")
    @RequireRole({ "BUYER", "PURCHASE_MANAGER", "ADMIN" })
    public ApiResponse<List<ApplicationSupplierResponse>> listSuppliers(@PathVariable Long id) {
        return ApiResponse.ok(applicationSupplierService.list(id));
    }

    /** 采购员/管理员为申请单录入一条候选供应商信息 */
    @PostMapping("/{id}/suppliers")
    @RequireRole({ "BUYER", "ADMIN" })
    public ApiResponse<ApplicationSupplierResponse> addSupplier(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationSupplierRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        return ApiResponse.ok(applicationSupplierService.add(id, request, currentUser.userId()));
    }

    /** 采购员/管理员编辑某条候选供应商信息 */
    @PutMapping("/{id}/suppliers/{candidateId}")
    @RequireRole({ "BUYER", "ADMIN" })
    public ApiResponse<ApplicationSupplierResponse> updateSupplier(
            @PathVariable Long id,
            @PathVariable Long candidateId,
            @Valid @RequestBody ApplicationSupplierRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        return ApiResponse.ok(
                applicationSupplierService.update(id, candidateId, request, currentUser.userId()));
    }

    /** 采购员/管理员删除某张申请单的一条候选供应商 */
    @DeleteMapping("/{id}/suppliers/{candidateId}")
    @RequireRole({ "BUYER", "ADMIN" })
    public ApiResponse<Void> deleteSupplier(
            @PathVariable Long id,
            @PathVariable Long candidateId) {
        applicationSupplierService.delete(id, candidateId);
        return ApiResponse.ok();
    }

    /** 采购负责人/管理员选定某条候选供应商为中标供应商 */
    @PutMapping("/{id}/suppliers/{candidateId}/select")
    @RequireRole({ "PURCHASE_MANAGER", "ADMIN" })
    public ApiResponse<Void> selectSupplier(
            @PathVariable Long id,
            @PathVariable Long candidateId,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        applicationSupplierService.select(id, candidateId, currentUser.userId());
        return ApiResponse.ok();
    }

    /** 采购负责人/管理员取消选定某条候选供应商 */
    @PutMapping("/{id}/suppliers/{candidateId}/unselect")
    @RequireRole({ "PURCHASE_MANAGER", "ADMIN" })
    public ApiResponse<Void> unselectSupplier(
            @PathVariable Long id,
            @PathVariable Long candidateId,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        applicationSupplierService.unselect(id, candidateId, currentUser.userId());
        return ApiResponse.ok();
    }

    @PostMapping
    @RequireRole({ "APPLICANT" })
    public ApiResponse<ApplicationResponse> createDraft(
            @Valid @RequestBody ApplicationCreateRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        return ApiResponse.ok(applicationService.createDraft(request, currentUser.userId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.getApplication(id));
    }

    @GetMapping
    public ApiResponse<PageResult<ApplicationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Boolean supplierSelected) {
        return ApiResponse.ok(applicationService.listApplications(page, size, keyword, status, supplierSelected));
    }

    @PostMapping("/{id}/submit")
    @RequireRole({ "APPLICANT" })
    public ApiResponse<ApplicationResponse> submit(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        return ApiResponse.ok(applicationService.submit(id, currentUser.userId()));
    }

    @PostMapping("/{id}/approve")
    @RequireRole({ "APPROVER", "ADMIN" })
    public ApiResponse<ApplicationResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ApproveRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        boolean isAdmin = currentUser.roleCodes() != null
                && currentUser.roleCodes().contains("ADMIN");
        return ApiResponse.ok(applicationService.approve(
                id, request == null ? null : request.comment(), currentUser.userId(), isAdmin));
    }

    @PostMapping("/{id}/reject")
    @RequireRole({ "APPROVER", "ADMIN" })
    public ApiResponse<ApplicationResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        boolean isAdmin = currentUser.roleCodes() != null
                && currentUser.roleCodes().contains("ADMIN");
        return ApiResponse.ok(applicationService.reject(
                id, request.comment(), currentUser.userId(), isAdmin));
    }

    @PostMapping("/{id}/reapply")
    @RequireRole({ "APPLICANT", "ADMIN" })
    public ApiResponse<ApplicationResponse> reapply(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        return ApiResponse.ok(applicationService.reapply(id, currentUser.userId()));
    }

    @PutMapping("/{id}")
    @RequireRole({ "APPLICANT", "ADMIN" })
    public ApiResponse<ApplicationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationCreateRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        boolean isAdmin = currentUser.roleCodes() != null
                && currentUser.roleCodes().contains("ADMIN");
        return ApiResponse.ok(applicationService.update(id, request, currentUser.userId(), isAdmin));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ "ADMIN" })
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) LoginResponse currentUser) {
        applicationService.delete(id, currentUser.userId());
        return ApiResponse.ok();
    }
}
