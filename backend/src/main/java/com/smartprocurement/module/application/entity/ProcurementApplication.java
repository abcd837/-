package com.smartprocurement.module.application.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.smartprocurement.module.application.enums.ApplicationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("procurement_application")
public class ProcurementApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String applicationNo;

    private String title;

    private Long applicantId;

    private Long approverId;

    private Long departmentId;

    private String applicationType = "NORMAL";

    private String purpose;

    private String budgetAccountCode;

    private String budgetAccountName;

    private BigDecimal budgetAvailableAmount;

    private BigDecimal totalAmount;

    private String currency = "CNY";

    private LocalDate requiredDate;

    private ApplicationStatus status = ApplicationStatus.DRAFT;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    /** 审批意见：通过或驳回时填写，驳回必填 */
    private String approvalComment;

    private LocalDateTime withdrawnAt;

    @Version
    private Integer version = 1;

    private Long createdBy;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
