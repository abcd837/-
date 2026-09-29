package com.smartprocurement.module.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.application.entity.ProcurementApplication;
import com.smartprocurement.module.application.mapper.ProcurementApplicationMapper;
import com.smartprocurement.module.supplier.dto.SupplierResponse;
import com.smartprocurement.module.supplier.dto.SupplierSaveRequest;
import com.smartprocurement.module.supplier.entity.Supplier;
import com.smartprocurement.module.supplier.mapper.SupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final ProcurementApplicationMapper applicationMapper;

    public SupplierService(SupplierMapper supplierMapper,
            ProcurementApplicationMapper applicationMapper) {
        this.supplierMapper = supplierMapper;
        this.applicationMapper = applicationMapper;
    }

    /**
     * 供应商列表。keyword 在编码/名称/联系人里模糊匹配。
     */
    @Transactional(readOnly = true)
    public List<SupplierResponse> list(String keyword) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<Supplier>()
                .isNull(Supplier::getDeletedAt)
                .orderByDesc(Supplier::getCreatedAt)
                .orderByDesc(Supplier::getId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Supplier::getSupplierNo, kw)
                    .or().like(Supplier::getName, kw)
                    .or().like(Supplier::getContactPerson, kw));
        }
        List<Supplier> suppliers = supplierMapper.selectList(wrapper);
        Map<Long, String> appNoMap = applicationNoMap(suppliers);
        return suppliers.stream()
                .map(s -> SupplierResponse.from(s, appNoMap.get(s.getApplicationId())))
                .toList();
    }

    @Transactional
    public SupplierResponse create(SupplierSaveRequest request, Long operatorId) {
        if (existsActiveByNo(request.supplierNo(), null)) {
            throw new BusinessException("供应商编码已存在");
        }
        Supplier supplier = new Supplier();
        applyFields(supplier, request);
        supplier.setStatus(request.status() == null ? 1 : request.status());
        supplier.setCreatedBy(operatorId);
        supplier.setUpdatedBy(operatorId);
        supplierMapper.insert(supplier);
        return SupplierResponse.from(supplier, applicationNoMap(supplier).get(supplier.getApplicationId()));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierSaveRequest request, Long operatorId) {
        Supplier supplier = getActiveSupplier(id);
        if (existsActiveByNo(request.supplierNo(), id)) {
            throw new BusinessException("供应商编码已存在");
        }
        applyFields(supplier, request);
        if (request.status() != null) {
            supplier.setStatus(request.status());
        }
        supplier.setUpdatedBy(operatorId);
        supplierMapper.updateById(supplier);
        return SupplierResponse.from(supplier, applicationNoMap(supplier).get(supplier.getApplicationId()));
    }

    /** 软删除 */
    @Transactional
    public void delete(Long id) {
        Supplier supplier = getActiveSupplier(id);
        supplier.setDeletedAt(LocalDateTime.now());
        supplierMapper.updateById(supplier);
    }

    private void applyFields(Supplier supplier, SupplierSaveRequest request) {
        supplier.setSupplierNo(request.supplierNo());
        supplier.setName(request.name());
        supplier.setShortName(request.shortName());
        supplier.setContactPerson(request.contactPerson());
        supplier.setContactPhone(request.contactPhone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setRemark(request.remark());
        supplier.setApplicationId(request.applicationId());
    }

    /** 批量构建 applicationId → applicationNo 的映射 */
    private Map<Long, String> applicationNoMap(List<Supplier> suppliers) {
        Set<Long> ids = suppliers.stream()
                .map(Supplier::getApplicationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return doQueryAppNo(ids);
    }

    private Map<Long, String> applicationNoMap(Supplier supplier) {
        return doQueryAppNo(supplier.getApplicationId() == null
                ? Set.of()
                : Set.of(supplier.getApplicationId()));
    }

    private Map<Long, String> doQueryAppNo(Set<Long> ids) {
        if (ids.isEmpty()) {
            // 不能用 Map.of()：其 get(null) 会抛 NPE，而 supplier.applicationId 可能为 null
            return new HashMap<>();
        }
        return applicationMapper.selectList(
                new LambdaQueryWrapper<ProcurementApplication>()
                        .in(ProcurementApplication::getId, ids)
                        .isNull(ProcurementApplication::getDeletedAt))
                .stream()
                .collect(Collectors.toMap(
                        ProcurementApplication::getId,
                        ProcurementApplication::getApplicationNo));
    }

    private boolean existsActiveByNo(String supplierNo, Long excludeId) {
        Long count = supplierMapper.selectCount(
                new LambdaQueryWrapper<Supplier>()
                        .eq(Supplier::getSupplierNo, supplierNo)
                        .isNull(Supplier::getDeletedAt)
                        .ne(excludeId != null, Supplier::getId, excludeId));
        return count > 0;
    }

    private Supplier getActiveSupplier(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null || supplier.getDeletedAt() != null) {
            throw new BusinessException("供应商不存在");
        }
        return supplier;
    }
}
