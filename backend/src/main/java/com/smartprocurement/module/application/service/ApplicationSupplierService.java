package com.smartprocurement.module.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.application.dto.ApplicationSupplierRequest;
import com.smartprocurement.module.application.dto.ApplicationSupplierResponse;
import com.smartprocurement.module.application.entity.ProcurementApplication;
import com.smartprocurement.module.application.entity.ProcurementApplicationSupplier;
import com.smartprocurement.module.application.mapper.ProcurementApplicationMapper;
import com.smartprocurement.module.application.mapper.ProcurementApplicationSupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 采购申请候选供应商（询价名单）管理。
 * 采购员为申请单手动录入候选供应商及报价，录入阶段不强制关联供应商档案。
 */
@Service
public class ApplicationSupplierService {

    private final ProcurementApplicationSupplierMapper candidateMapper;
    private final ProcurementApplicationMapper applicationMapper;

    public ApplicationSupplierService(
            ProcurementApplicationSupplierMapper candidateMapper,
            ProcurementApplicationMapper applicationMapper) {
        this.candidateMapper = candidateMapper;
        this.applicationMapper = applicationMapper;
    }

    @Transactional(readOnly = true)
    public List<ApplicationSupplierResponse> list(Long applicationId) {
        requireApplication(applicationId);
        return candidateMapper.selectList(
                new LambdaQueryWrapper<ProcurementApplicationSupplier>()
                        .eq(ProcurementApplicationSupplier::getApplicationId, applicationId)
                        .isNull(ProcurementApplicationSupplier::getDeletedAt)
                        .orderByAsc(ProcurementApplicationSupplier::getQuotedAmount)
                        .orderByAsc(ProcurementApplicationSupplier::getId))
                .stream()
                .map(ApplicationSupplierResponse::from)
                .toList();
    }

    @Transactional
    public ApplicationSupplierResponse add(Long applicationId, ApplicationSupplierRequest request, Long operatorId) {
        requireApplication(applicationId);
        requireNotSelected(applicationId);
        ProcurementApplicationSupplier candidate = new ProcurementApplicationSupplier();
        candidate.setApplicationId(applicationId);
        candidate.setSupplierName(request.supplierName().trim());
        candidate.setContactPerson(request.contactPerson());
        candidate.setContactPhone(request.contactPhone());
        candidate.setQuotedAmount(request.quotedAmount());
        candidate.setDeliveryDays(request.deliveryDays());
        candidate.setRemark(request.remark());
        candidate.setIsSelected(0);
        candidate.setCreatedBy(operatorId);
        candidate.setUpdatedBy(operatorId);
        candidateMapper.insert(candidate);
        return ApplicationSupplierResponse.from(candidate);
    }

    @Transactional
    public ApplicationSupplierResponse update(Long applicationId, Long candidateId,
            ApplicationSupplierRequest request, Long operatorId) {
        requireNotSelected(applicationId);
        ProcurementApplicationSupplier candidate = candidateMapper.selectById(candidateId);
        if (candidate == null
                || candidate.getDeletedAt() != null
                || !candidate.getApplicationId().equals(applicationId)) {
            throw new BusinessException("候选供应商不存在");
        }
        candidate.setSupplierName(request.supplierName().trim());
        candidate.setContactPerson(request.contactPerson());
        candidate.setContactPhone(request.contactPhone());
        candidate.setQuotedAmount(request.quotedAmount());
        candidate.setDeliveryDays(request.deliveryDays());
        candidate.setRemark(request.remark());
        candidate.setUpdatedBy(operatorId);
        candidateMapper.updateById(candidate);
        return ApplicationSupplierResponse.from(candidate);
    }

    @Transactional
    public void delete(Long applicationId, Long candidateId) {
        requireNotSelected(applicationId);
        ProcurementApplicationSupplier candidate = candidateMapper.selectById(candidateId);
        if (candidate == null
                || candidate.getDeletedAt() != null
                || !candidate.getApplicationId().equals(applicationId)) {
            throw new BusinessException("候选供应商不存在");
        }
        candidate.setDeletedAt(java.time.LocalDateTime.now());
        candidateMapper.updateById(candidate);
    }

    /**
     * 选定某条候选供应商为中标供应商（采购负责人/管理员操作）。
     * 同一申请单下只能有一条 is_selected = 1。
     */
    @Transactional
    public void select(Long applicationId, Long candidateId, Long operatorId) {
        requireApplication(applicationId);
        ProcurementApplicationSupplier candidate = candidateMapper.selectById(candidateId);
        if (candidate == null
                || candidate.getDeletedAt() != null
                || !candidate.getApplicationId().equals(applicationId)) {
            throw new BusinessException("候选供应商不存在");
        }
        // 先把该申请单下所有候选供应商的 is_selected 置为 0
        List<ProcurementApplicationSupplier> all = candidateMapper.selectList(
                new LambdaQueryWrapper<ProcurementApplicationSupplier>()
                        .eq(ProcurementApplicationSupplier::getApplicationId, applicationId)
                        .isNull(ProcurementApplicationSupplier::getDeletedAt));
        for (ProcurementApplicationSupplier s : all) {
            if (s.getIsSelected() != null && s.getIsSelected() == 1) {
                s.setIsSelected(0);
                s.setUpdatedBy(operatorId);
                candidateMapper.updateById(s);
            }
        }
        // 再把目标供应商置为 1
        candidate.setIsSelected(1);
        candidate.setUpdatedBy(operatorId);
        candidateMapper.updateById(candidate);
    }

    /**
     * 取消选定某条候选供应商（采购负责人/管理员操作）。
     */
    @Transactional
    public void unselect(Long applicationId, Long candidateId, Long operatorId) {
        ProcurementApplicationSupplier candidate = candidateMapper.selectById(candidateId);
        if (candidate == null
                || candidate.getDeletedAt() != null
                || !candidate.getApplicationId().equals(applicationId)) {
            throw new BusinessException("候选供应商不存在");
        }
        if (candidate.getIsSelected() == null || candidate.getIsSelected() != 1) {
            throw new BusinessException("该供应商未被选定");
        }
        candidate.setIsSelected(0);
        candidate.setUpdatedBy(operatorId);
        candidateMapper.updateById(candidate);
    }

    private void requireApplication(Long applicationId) {
        ProcurementApplication application = applicationMapper.selectById(applicationId);
        if (application == null || application.getDeletedAt() != null) {
            throw new BusinessException("采购申请不存在");
        }
    }

    /** 定标即封标：已选定中标供应商的申请单，询价名单冻结，不允许再增删改 */
    private void requireNotSelected(Long applicationId) {
        Long count = candidateMapper.selectCount(
                new LambdaQueryWrapper<ProcurementApplicationSupplier>()
                        .eq(ProcurementApplicationSupplier::getApplicationId, applicationId)
                        .eq(ProcurementApplicationSupplier::getIsSelected, 1)
                        .isNull(ProcurementApplicationSupplier::getDeletedAt));
        if (count != null && count > 0) {
            throw new BusinessException("已选定中标供应商，询价名单已冻结，如需调整请先取消选定");
        }
    }
}
