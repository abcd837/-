package com.smartprocurement.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.auth.dto.LoginRequest;
import com.smartprocurement.module.auth.dto.LoginResponse;
import com.smartprocurement.module.user.entity.SysUser;
import com.smartprocurement.module.user.entity.SysUserRole;
import com.smartprocurement.module.user.mapper.SysUserMapper;
import com.smartprocurement.module.user.mapper.SysUserRoleMapper;
import com.smartprocurement.module.role.entity.SysRole;
import com.smartprocurement.module.role.mapper.SysRoleMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final TokenStore tokenStore;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            SysUserMapper userMapper,
            SysUserRoleMapper userRoleMapper,
            SysRoleMapper roleMapper,
            TokenStore tokenStore
    ) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.tokenStore = tokenStore;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.username())
                        .isNull(SysUser::getDeletedAt)
        );
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已停用");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }

        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>()
                                .eq(SysUserRole::getUserId, user.getId())
                )
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();

        List<String> roleCodes = roleIds.isEmpty()
                ? List.of()
                : roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getCode).toList();

        String token = UUID.randomUUID().toString().replace("-", "");
        LoginResponse response = new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartmentId(),
                roleIds,
                roleCodes
        );
        tokenStore.put(token, response);
        return response;
    }

    public LoginResponse me(String token) {
        LoginResponse response = tokenStore.get(token);
        if (response == null) {
            throw new BusinessException(40100, "登录已失效");
        }
        return response;
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenStore.remove(token);
        }
    }
}
