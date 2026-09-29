package com.smartprocurement.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartprocurement.module.application.dto.ApplicationListRow;
import com.smartprocurement.module.application.entity.ProcurementApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProcurementApplicationMapper extends BaseMapper<ProcurementApplication> {

    /**
     * 采购申请列表联表分页查询。
     * 名称字段（申请人/审批人/部门）由数据库 JOIN 直接带出。
     */
    IPage<ApplicationListRow> selectApplicationPage(
            Page<ApplicationListRow> page,
            @Param("keyword") String keyword,
            @Param("statusCode") String statusCode,
            @Param("supplierSelected") Boolean supplierSelected);
}
