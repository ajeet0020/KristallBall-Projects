package com.millity.assets.config;

import com.millity.assets.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "request_start_time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        String actorInfo = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
            actorInfo = principal.username() + "(" + principal.role() + ",base=" + principal.baseId() + ")";
        }

        if (ex != null) {
            log.warn("API {} {} completed with status {} in {}ms for user [{}] with exception: {}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration, actorInfo, ex.getMessage());
        } else {
            log.info("API {} {} completed with status {} in {}ms for user [{}]",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration, actorInfo);
        }
    }
}
