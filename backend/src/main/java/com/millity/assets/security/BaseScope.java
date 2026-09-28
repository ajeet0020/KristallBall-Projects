package com.millity.assets.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("baseScope")
public class BaseScope {
    public boolean allows(Authentication authentication, Long baseId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return true;
        Object principal = authentication.getPrincipal();
        return principal instanceof JwtPrincipal jwtPrincipal && jwtPrincipal.baseId() != null && jwtPrincipal.baseId().equals(baseId);
    }
}
