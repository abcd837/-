package com.smartprocurement.config;

import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.auth.service.AuthService;
import com.smartprocurement.module.auth.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(40100, "请先登录");
        }

        String token = authorization.substring(7);
        LoginResponse currentUser = authService.me(token);

        if (handler instanceof HandlerMethod handlerMethod) {
            RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(),
                    RequireRole.class
            );
            if (requireRole == null) {
                requireRole = AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getBeanType(),
                        RequireRole.class
                );
            }

            if (requireRole != null && !hasAnyRole(currentUser, requireRole.value())) {
                throw new BusinessException(40300, "没有权限执行该操作");
            }
        }

        request.setAttribute(CURRENT_USER_ATTRIBUTE, currentUser);
        return true;
    }

    private boolean hasAnyRole(LoginResponse currentUser, String[] requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }

        Set<String> userRoles = Set.copyOf(currentUser.roleCodes());
        return Arrays.stream(requiredRoles).anyMatch(userRoles::contains);
    }
}
