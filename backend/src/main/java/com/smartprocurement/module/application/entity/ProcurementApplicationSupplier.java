package com.smartprocurement.module.application.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购申请候选供应商（询价名单）。
 * 采购员针对一张申请单手动录入若干家供应商的报价信息，供采购负责人遴选。
 */
@Data
@TableName("procurement_application_supplier")
public class ProcurementApplicationSupplier {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private String supplierName;

    private String contactPerson;

    private String contactPhone;

    private BigDecimal quotedAmount;

    /** 承诺交货周期（天） */
    private Integer deliveryDays;

    /** 是否被选定：1 是 0 否 */
    private Integer isSelected = 0;

    private String remark;

    private Long createdBy;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
