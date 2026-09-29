package com.smartprocurement.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.role.entity.SysRole;
import com.smartprocurement.module.role.mapper.SysRoleMapper;
import com.smartprocurement.module.user.dto.UserCreateRequest;
import com.smartprocurement.module.user.dto.UserResponse;
import com.smartprocurement.module.user.dto.UserUpdateRequest;
import com.smartprocurement.module.user.entity.SysUser;
import com.smartprocurement.module.user.entity.SysUserRole;
import com.smartprocurement.module.user.mapper.SysUserMapper;
import com.smartprocurement.module.user.mapper.SysUserRoleMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(
            SysUserMapper userMapper,
            SysUserRoleMapper userRoleMapper,
            SysRoleMapper roleMapper
    ) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    /** 查所有拥有 APPROVER 或 ADMIN 角色的用户（供发起人选择审批人） */
    @Transactional(readOnly = true)
    public List<UserResponse> listApprovers() {
        // 1. 查出 APPROVER 和 ADMIN 角色的 id
        List<Long> approverRoleIds = roleMapper.selectList(
                        new LambdaQueryWrapper<SysRole>()
                                .in(SysRole::getCode, "APPROVER", "ADMIN")
                ).stream()
                .map(SysRole::getId)
                .toList();

        if (approverRoleIds.isEmpty()) {
            return List.of();
        }

        // 2. 查出有这些角色的 user_id
        List<Long> userIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>()
                                .in(SysUserRole::getRoleId, approverRoleIds)
                ).stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return List.of();
        }

        // 3. 查这些用户
        return userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .in(SysUser::getId, userIds)
                                .isNull(SysUser::getDeletedAt)
                                .orderByAsc(SysUser::getDisplayName)
                ).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .isNull(SysUser::getDeletedAt)
                                .orderByDesc(SysUser::getCreatedAt)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (existsActiveByUserNo(request.userNo(), null)) {
            throw new BusinessException("用户编号已存在");
        }
        if (existsActiveByUsername(request.username(), null)) {
            throw new BusinessException("登录账号已存在");
        }

        SysUser user = new SysUser();
        user.setUserNo(request.userNo());
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setDepartmentId(request.departmentId());
        user.setEmail(request.email());
        user.setMobile(request.mobile());
        String rawPassword = StringUtils.hasText(request.password()) ? request.password() : "123456";
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(request.status() == null ? 1 : request.status());

        userMapper.insert(user);
        assignRoles(user.getId(), request.roleIds());
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        SysUser user = getActiveUser(id);

        if (existsActiveByUserNo(request.userNo(), id)) {
            throw new BusinessException("用户编号已存在");
        }
        if (existsActiveByUsername(request.username(), id)) {
            throw new BusinessException("登录账号已存在");
        }

        user.setUserNo(request.userNo());
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setDepartmentId(request.departmentId());
        user.setEmail(request.email());
        user.setMobile(request.mobile());
        user.setStatus(request.status() == null ? 1 : request.status());

        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        userMapper.updateById(user);
        assignRoles(user.getId(), request.roleIds());
        return toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = getActiveUser(id);
        user.setDeletedAt(LocalDateTime.now());
        userMapper.updateById(user);
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, id)
        );
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );

        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        for (Long roleId : roleIds.stream().distinct().toList()) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private boolean existsActiveByUserNo(String userNo, Long excludeId) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserNo, userNo)
                        .isNull(SysUser::getDeletedAt)
                        .ne(excludeId != null, SysUser::getId, excludeId)
        );
        return count > 0;
    }

    private boolean existsActiveByUsername(String username, Long excludeId) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .isNull(SysUser::getDeletedAt)
                        .ne(excludeId != null, SysUser::getId, excludeId)
        );
        return count > 0;
    }

    private SysUser getActiveUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private UserResponse toResponse(SysUser user) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>()
                                .eq(SysUserRole::getUserId, user.getId())
                )
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUserNo(),
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartmentId(),
                user.getEmail(),
                user.getMobile(),
                user.getStatus(),
                user.getCreatedAt(),
                roleIds
        );
    }
}
