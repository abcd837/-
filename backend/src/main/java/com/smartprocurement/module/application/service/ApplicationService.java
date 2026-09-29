package com.smartprocurement.module.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartprocurement.common.api.PageResult;
import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.application.dto.ApplicationCreateRequest;
import com.smartprocurement.module.application.dto.ApplicationDetailResponse;
import com.smartprocurement.module.application.dto.ApplicationItemRequest;
import com.smartprocurement.module.application.dto.ApplicationListRow;
import com.smartprocurement.module.application.dto.ApplicationResponse;
import com.smartprocurement.module.application.dto.AttachmentInfoRequest;
import com.smartprocurement.module.application.entity.ProcurementApplication;
import com.smartprocurement.module.application.entity.ProcurementApplicationItem;
import com.smartprocurement.module.application.enums.ApplicationStatus;
import com.smartprocurement.module.application.mapper.ProcurementApplicationItemMapper;
import com.smartprocurement.module.application.mapper.ProcurementApplicationMapper;
import com.smartprocurement.module.file.entity.ProcurementAttachment;
import com.smartprocurement.module.file.mapper.ProcurementAttachmentMapper;
import com.smartprocurement.module.file.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ProcurementApplicationMapper applicationMapper;
    private final ProcurementApplicationItemMapper itemMapper;
    private final ProcurementAttachmentMapper attachmentMapper;
    private final FileStorageService fileStorageService;

    public ApplicationService(
            ProcurementApplicationMapper applicationMapper,
            ProcurementApplicationItemMapper itemMapper,
            ProcurementAttachmentMapper attachmentMapper,
            FileStorageService fileStorageService) {
        this.applicationMapper = applicationMapper;
        this.itemMapper = itemMapper;
        this.attachmentMapper = attachmentMapper;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public ApplicationResponse createDraft(ApplicationCreateRequest request, Long applicantId) {
        if (request.title() == null) {
            throw new BusinessException("申请名称不能为空");
        }
        if (request.departmentId() == null) {
            throw new BusinessException("部门名称不能为空");
        }

        ProcurementApplication application = new ProcurementApplication();

        application.setApplicationNo(generateApplicationNo());
        application.setTitle(request.title());
        application.setApplicantId(applicantId);
        application.setApproverId(request.approverId());
        application.setDepartmentId(request.departmentId());
        application.setApplicationType(
                request.applicationType() == null ? "NORMAL" : request.applicationType());
        application.setPurpose(request.purpose());
        application.setCurrency(
                request.currency() == null ? "CNY" : request.currency());
        application.setRequiredDate(request.requiredDate());
        application.setCreatedBy(applicantId);
        application.setUpdatedBy(applicantId);

        int lineNo = 1;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ProcurementApplicationItem> items = new ArrayList<>();
        for (ApplicationItemRequest itemRequest : request.items()) {
            ProcurementApplicationItem item = toItem(itemRequest, lineNo++);
            if (item.getEstimatedAmount() != null) {
                totalAmount = totalAmount.add(item.getEstimatedAmount());
            }
            items.add(item);
        }
        application.setTotalAmount(totalAmount);
        applicationMapper.insert(application);

        for (ProcurementApplicationItem item : items) {
            item.setApplicationId(application.getId());
            itemMapper.insert(item);
        }

        if (request.attachments() != null) {
            for (AttachmentInfoRequest attachmentRequest : request.attachments()) {
                insertAttachment(application.getId(), attachmentRequest);
            }
        }

        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse getApplication(Long id) {
        ProcurementApplication application = getApplicationEntity(id);
        List<ProcurementApplicationItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ProcurementApplicationItem>()
                        .eq(ProcurementApplicationItem::getApplicationId, id)
                        .isNull(ProcurementApplicationItem::getDeletedAt)
                        .orderByAsc(ProcurementApplicationItem::getLineNo));
        List<ProcurementAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<ProcurementAttachment>()
                        .eq(ProcurementAttachment::getBusinessType, "PURCHASE_APPLICATION")
                        .eq(ProcurementAttachment::getBusinessId, id)
                        .orderByAsc(ProcurementAttachment::getCreatedAt));
        return ApplicationDetailResponse.from(application, items, attachments);
    }

    private ProcurementApplication getApplicationEntity(Long id) {
        ProcurementApplication application = applicationMapper.selectById(id);
        if (application == null || application.getDeletedAt() != null) {
            throw new BusinessException("采购申请不存在");
        }
        return application;
    }

    @Transactional(readOnly = true)
    public PageResult<ApplicationResponse> listApplications(
            int page, int size, String keyword, ApplicationStatus status, String supplierStage) {
        Page<ApplicationListRow> pageParam = new Page<>(page + 1L, size);
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String statusCode = status == null ? null : status.name();

        // 联表查询在 mapper XML 中完成，名称字段由数据库 JOIN 直接带出
        IPage<ApplicationListRow> result = applicationMapper.selectApplicationPage(pageParam, trimmedKeyword,
                statusCode, supplierStage);
        return PageResult.from(result, ApplicationResponse::fromRow);
    }

    @Transactional
    public ApplicationResponse submit(Long id, Long operatorId) {
        ProcurementApplication application = getApplicationEntity(id);
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessException("只有草稿状态可以提交");
        }
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(LocalDateTime.now());
        application.setUpdatedBy(operatorId);
        applicationMapper.updateById(application);
        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse approve(Long id, String comment, Long operatorId, boolean isAdmin) {
        ProcurementApplication application = getApplicationEntity(id);
        if (application.getStatus() != ApplicationStatus.SUBMITTED
                && application.getStatus() != ApplicationStatus.PENDING_APPROVAL) {
            throw new BusinessException("当前状态不允许审批");
        }
        // 有指定审批人时，只有被指定的人或 ADMIN 能审批
        if (application.getApproverId() != null
                && !application.getApproverId().equals(operatorId)
                && !isAdmin) {
            throw new BusinessException("此申请已指定审批人，只有该审批人可以操作");
        }
        application.setStatus(ApplicationStatus.APPROVED);
        application.setApprovedAt(LocalDateTime.now());
        if (comment != null && !comment.isBlank()) {
            application.setApprovalComment(comment.trim());
        }
        application.setUpdatedBy(operatorId);
        applicationMapper.updateById(application);
        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse reject(Long id, String comment, Long operatorId, boolean isAdmin) {
        ProcurementApplication application = getApplicationEntity(id);
        if (application.getStatus() != ApplicationStatus.SUBMITTED
                && application.getStatus() != ApplicationStatus.PENDING_APPROVAL) {
            throw new BusinessException("当前状态不允许驳回");
        }
        if (application.getApproverId() != null
                && !application.getApproverId().equals(operatorId)
                && !isAdmin) {
            throw new BusinessException("此申请已指定审批人，只有该审批人可以操作");
        }
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectedAt(LocalDateTime.now());
        // DTO 已用 @NotBlank 保证非空，这里 trim 后落库
        application.setApprovalComment(comment.trim());
        application.setUpdatedBy(operatorId);
        applicationMapper.updateById(application);
        return ApplicationResponse.from(application);
    }

    private ProcurementApplicationItem toItem(ApplicationItemRequest request, int lineNo) {
        ProcurementApplicationItem item = new ProcurementApplicationItem();
        item.setLineNo(lineNo);
        item.setMaterialCode(request.materialCode());
        item.setMaterialName(request.materialName());
        item.setSpecification(request.specification());
        item.setUnit(request.unit());
        item.setQuantity(request.quantity());
        item.setEstimatedUnitPrice(request.estimatedUnitPrice());
        item.setEstimatedAmount(request.estimatedAmount());
        item.setRequiredDate(request.requiredDate());
        item.setSuggestSupplierCode(request.suggestSupplierCode());
        item.setSuggestSupplierName(request.suggestSupplierName());
        item.setRemark(request.remark());
        return item;
    }

    /**
     * 更新申请。普通用户只能编辑 DRAFT 状态；
     * 管理员可编辑非终态申请，但已通过（APPROVED）的申请任何角色都不能编辑。
     */
    @Transactional
    public ApplicationResponse update(Long id, ApplicationCreateRequest request, Long operatorId, boolean isAdmin) {
        ProcurementApplication application = getApplicationEntity(id);

        // 已通过的申请任何角色都不能编辑，保证审批结果的严肃性（与删除规则保持一致）
        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new BusinessException("已通过的采购申请不能编辑");
        }

        if (!isAdmin && application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessException("只有草稿状态可以编辑");
        }

        application.setTitle(request.title());
        application.setApproverId(request.approverId());
        application.setDepartmentId(request.departmentId());
        application.setApplicationType(
                request.applicationType() == null ? "NORMAL" : request.applicationType());
        application.setPurpose(request.purpose());
        application.setRequiredDate(request.requiredDate());
        application.setUpdatedBy(operatorId);

        // 重新计算金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ApplicationItemRequest itemRequest : request.items()) {
            if (itemRequest.estimatedAmount() != null) {
                totalAmount = totalAmount.add(itemRequest.estimatedAmount());
            }
        }
        application.setTotalAmount(totalAmount);
        applicationMapper.updateById(application);

        // 明细全删重插（最简单且可靠的方式）
        itemMapper.delete(new LambdaQueryWrapper<ProcurementApplicationItem>()
                .eq(ProcurementApplicationItem::getApplicationId, id));

        int lineNo = 1;
        for (ApplicationItemRequest itemRequest : request.items()) {
            ProcurementApplicationItem item = toItem(itemRequest, lineNo++);
            item.setApplicationId(id);
            itemMapper.insert(item);
        }

        // 附件与明细一样按“前端回传完整列表”对账：保留带 id 的、插入新的、删除被移除的
        reconcileAttachments(id, request.attachments());

        return ApplicationResponse.from(application);
    }

    private void insertAttachment(Long applicationId, AttachmentInfoRequest req) {
        ProcurementAttachment attachment = new ProcurementAttachment();
        attachment.setBusinessType("PURCHASE_APPLICATION");
        attachment.setBusinessId(applicationId);
        attachment.setFileName(req.fileName());
        attachment.setStoredName(req.storedName());
        attachment.setFilePath(req.filePath());
        attachment.setFileSize(req.fileSize());
        attachment.setContentType(req.contentType());
        attachmentMapper.insert(attachment);
    }

    /**
     * 附件对账更新。
     * 前端提交的是“编辑后应该存在的完整附件列表”：
     * - 带 id 的是用户保留下来的已有附件，数据库记录不动；
     * - id 为空的是本次新上传的附件，插入新记录；
     * - 数据库里存在但不在提交列表中的，删除记录并在事务提交后清理物理文件。
     */
    private void reconcileAttachments(Long applicationId, List<AttachmentInfoRequest> requested) {
        List<ProcurementAttachment> existing = attachmentMapper.selectList(
                new LambdaQueryWrapper<ProcurementAttachment>()
                        .eq(ProcurementAttachment::getBusinessType, "PURCHASE_APPLICATION")
                        .eq(ProcurementAttachment::getBusinessId, applicationId));

        Set<Long> keptIds = requested == null
                ? Set.of()
                : requested.stream()
                        .map(AttachmentInfoRequest::id)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        List<ProcurementAttachment> removed = new ArrayList<>();
        for (ProcurementAttachment attachment : existing) {
            if (!keptIds.contains(attachment.getId())) {
                attachmentMapper.deleteById(attachment.getId());
                removed.add(attachment);
            }
        }

        if (requested != null) {
            for (AttachmentInfoRequest req : requested) {
                if (req.id() != null) {
                    continue;
                }
                insertAttachment(applicationId, req);
            }
        }

        // 物理文件删除延后到事务提交之后：若事务回滚，数据库记录恢复，文件不能提前被删
        if (!removed.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (ProcurementAttachment attachment : removed) {
                        try {
                            fileStorageService.deletePhysicalFile(attachment);
                        } catch (Exception ex) {
                            // 磁盘文件被占用或已丢失：只记日志，不影响已提交的业务数据，可由对账任务后续清理
                            log.warn("附件物理文件删除失败 storedName={}: {}",
                                    attachment.getStoredName(), ex.getMessage());
                        }
                    }
                }
            });
        }
    }

    /** 软删除申请：把 deleted_at 置为当前时间。已通过的申请不允许删除。 */
    @Transactional
    public void delete(Long id, Long operatorId) {
        ProcurementApplication application = getApplicationEntity(id);

        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new BusinessException("已通过的申请不允许删除");
        }

        application.setDeletedAt(LocalDateTime.now());
        application.setUpdatedBy(operatorId);
        applicationMapper.updateById(application);

        // 明细也软删
        itemMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcurementApplicationItem>()
                        .eq(ProcurementApplicationItem::getApplicationId, id)
                        .isNull(ProcurementApplicationItem::getDeletedAt)
                        .set(ProcurementApplicationItem::getDeletedAt, LocalDateTime.now()));
    }

    /**
     * 驳回后申请人可以基于原申请数据一键重新发起，生成新的草稿。
     * 只复制业务字段（标题、部门、事由、明细等），
     * 状态归零为 DRAFT，生成新的申请单号和创建时间。
     */
    @Transactional
    public ApplicationResponse reapply(Long originalId, Long applicantId) {
        ProcurementApplication original = getApplicationEntity(originalId);

        // 只有被驳回或已撤回的申请可以重新申请
        if (original.getStatus() != ApplicationStatus.REJECTED
                && original.getStatus() != ApplicationStatus.WITHDRAWN) {
            throw new BusinessException("当前状态不允许重新申请");
        }

        // 只有原申请人本人可以重新申请
        if (!original.getApplicantId().equals(applicantId)) {
            throw new BusinessException("只能对自己的申请重新发起");
        }

        // 复制主表
        ProcurementApplication copy = new ProcurementApplication();
        copy.setApplicationNo(generateApplicationNo());
        copy.setTitle(original.getTitle());
        copy.setApplicantId(applicantId);
        copy.setDepartmentId(original.getDepartmentId());
        copy.setApplicationType(original.getApplicationType());
        copy.setPurpose(original.getPurpose());
        copy.setCurrency(original.getCurrency());
        copy.setRequiredDate(original.getRequiredDate());
        copy.setTotalAmount(original.getTotalAmount());
        copy.setBudgetAccountCode(original.getBudgetAccountCode());
        copy.setBudgetAccountName(original.getBudgetAccountName());
        copy.setBudgetAvailableAmount(original.getBudgetAvailableAmount());
        copy.setStatus(ApplicationStatus.DRAFT);
        copy.setSubmittedAt(null);
        copy.setApprovedAt(null);
        copy.setRejectedAt(null);
        copy.setWithdrawnAt(null);
        copy.setCreatedBy(applicantId);
        copy.setUpdatedBy(applicantId);
        applicationMapper.insert(copy);

        // 复制明细
        List<ProcurementApplicationItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ProcurementApplicationItem>()
                        .eq(ProcurementApplicationItem::getApplicationId, originalId)
                        .isNull(ProcurementApplicationItem::getDeletedAt));
        for (ProcurementApplicationItem item : items) {
            ProcurementApplicationItem copyItem = new ProcurementApplicationItem();
            copyItem.setApplicationId(copy.getId());
            copyItem.setLineNo(item.getLineNo());
            copyItem.setMaterialCode(item.getMaterialCode());
            copyItem.setMaterialName(item.getMaterialName());
            copyItem.setSpecification(item.getSpecification());
            copyItem.setUnit(item.getUnit());
            copyItem.setQuantity(item.getQuantity());
            copyItem.setEstimatedUnitPrice(item.getEstimatedUnitPrice());
            copyItem.setEstimatedAmount(item.getEstimatedAmount());
            copyItem.setRequiredDate(item.getRequiredDate());
            copyItem.setSuggestSupplierCode(item.getSuggestSupplierCode());
            copyItem.setSuggestSupplierName(item.getSuggestSupplierName());
            copyItem.setRemark(item.getRemark());
            itemMapper.insert(copyItem);
        }

        return ApplicationResponse.from(copy);
    }

    private String generateApplicationNo() {
        String date = LocalDateTime.now().format(DATE_FORMAT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PR-" + date + "-" + suffix;
    }
}
