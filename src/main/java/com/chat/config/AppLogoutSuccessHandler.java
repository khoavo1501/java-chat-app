package com.chat.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.chat.service.PresenceService;

@Component
public class AppLogoutSuccessHandler implements LogoutSuccessHandler {

    private final PresenceService presenceService;

    public AppLogoutSuccessHandler(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        if (authentication != null && authentication.getName() != null) {
            presenceService.forceOffline(authentication.getName());
        }

        response.sendRedirect(request.getContextPath() + "/login?logout");
    }
}
