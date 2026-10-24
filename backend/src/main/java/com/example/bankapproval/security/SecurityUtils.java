package com.example.bankapproval.security;

import com.example.bankapproval.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static DemoPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof DemoPrincipal)) {
            throw new BizException("UNAUTHENTICATED", "用户未登录");
        }
        return (DemoPrincipal) authentication.getPrincipal();
    }
}
