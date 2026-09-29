package com.smartprocurement.module.department.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.department.dto.DepartmentCreateRequest;
import com.smartprocurement.module.department.dto.DepartmentResponse;
import com.smartprocurement.module.department.dto.DepartmentUpdateRequest;
import com.smartprocurement.module.department.entity.SysDepartment;
import com.smartprocurement.module.department.mapper.SysDepartmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DepartmentService {

    private final SysDepartmentMapper departmentMapper;

    public DepartmentService(SysDepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        return departmentMapper.selectList(
                        new LambdaQueryWrapper<SysDepartment>()
                                .eq(SysDepartment::getStatus, 1)
                                .isNull(SysDepartment::getDeletedAt)
                                .orderByAsc(SysDepartment::getSortOrder)
                                .orderByAsc(SysDepartment::getId)
                )
                .stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        if (existsActiveByCode(request.code(), null)) {
            throw new BusinessException("部门编码已存在");
        }

        SysDepartment department = new SysDepartment();
        department.setCode(request.code());
        department.setName(request.name());
        department.setParentId(request.parentId());
        department.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        department.setStatus(1);
        departmentMapper.insert(department);
        return DepartmentResponse.from(department);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpdateRequest request) {
        SysDepartment department = getActiveDepartment(id);

        if (existsActiveByCode(request.code(), id)) {
            throw new BusinessException("部门编码已存在");
        }

        department.setCode(request.code());
        department.setName(request.name());
        department.setParentId(request.parentId());
        department.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        departmentMapper.updateById(department);
        return DepartmentResponse.from(department);
    }

    @Transactional
    public void delete(Long id) {
        SysDepartment department = getActiveDepartment(id);

        Long childCount = departmentMapper.selectCount(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getParentId, id)
                        .isNull(SysDepartment::getDeletedAt)
        );
        if (childCount > 0) {
            throw new BusinessException("存在下级部门，不能删除");
        }

        department.setDeletedAt(LocalDateTime.now());
        departmentMapper.updateById(department);
    }

    private boolean existsActiveByCode(String code, Long excludeId) {
        Long count = departmentMapper.selectCount(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getCode, code)
                        .isNull(SysDepartment::getDeletedAt)
                        .ne(excludeId != null, SysDepartment::getId, excludeId)
        );
        return count > 0;
    }

    private SysDepartment getActiveDepartment(Long id) {
        SysDepartment department = departmentMapper.selectById(id);
        if (department == null || department.getDeletedAt() != null) {
            throw new BusinessException("部门不存在");
        }
        return department;
    }
}
