package com.smartprocurement.module.role.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.role.dto.RoleCreateRequest;
import com.smartprocurement.module.role.dto.RoleResponse;
import com.smartprocurement.module.role.dto.RoleUpdateRequest;
import com.smartprocurement.module.role.entity.SysRole;
import com.smartprocurement.module.role.mapper.SysRoleMapper;
import com.smartprocurement.module.user.entity.SysUserRole;
import com.smartprocurement.module.user.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public RoleService(
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper
    ) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleMapper.selectList(
                        new LambdaQueryWrapper<SysRole>()
                                .isNull(SysRole::getDeletedAt)
                                .orderByDesc(SysRole::getCreatedAt)
                )
                .stream()
                .map(RoleResponse::from)
                .toList();
    }

    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        if (existsActiveByCode(request.code(), null)) {
            throw new BusinessException("角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setStatus(1);
        roleMapper.insert(role);
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        SysRole role = getActiveRole(id);

        if (existsActiveByCode(request.code(), id)) {
            throw new BusinessException("角色编码已存在");
        }

        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setStatus(request.status() == null ? 1 : request.status());
        roleMapper.updateById(role);
        return RoleResponse.from(role);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = getActiveRole(id);

        Long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, id)
        );
        if (userCount > 0) {
            throw new BusinessException("该角色已分配给用户，不能删除");
        }

        role.setDeletedAt(LocalDateTime.now());
        roleMapper.updateById(role);
    }

    private boolean existsActiveByCode(String code, Long excludeId) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getCode, code)
                        .isNull(SysRole::getDeletedAt)
                        .ne(excludeId != null, SysRole::getId, excludeId)
        );
        return count > 0;
    }

    private SysRole getActiveRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null || role.getDeletedAt() != null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }
}
