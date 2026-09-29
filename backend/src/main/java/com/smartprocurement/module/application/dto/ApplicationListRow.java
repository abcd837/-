package com.smartprocurement.module.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购申请列表联表查询行映射。
 * 由 ProcurementApplicationMapper.xml 的 selectApplicationPage 直接映射，
 * 名称字段（申请人/审批人/部门）由数据库 JOIN 带出。
 */
@Data
public class ApplicationListRow {
    private Long id;
    private String applicationNo;
    private String title;
    private Long applicantId;
    private String applicantName;
    private Long approverId;
    private String approverName;
    private Long departmentId;
    private String departmentName;
    private String applicationType;
    private String purpose;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate requiredDate;
    private String status;
    private String approvalComment;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
