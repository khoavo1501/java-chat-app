package com.chat.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.chat.dto.UserProfileResponse;
import com.chat.service.UserService;

@Controller
public class AdminPageController {

    private final UserService userService;

    public AdminPageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin")
    public String adminEntryPage() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboardPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        applyCommonAttributes(principal, model, "dashboard");
        return "admin-dashboard";
    }

    @GetMapping("/admin/users")
    public String adminUsersPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        applyCommonAttributes(principal, model, "users");
        return "admin";
    }

    @GetMapping("/admin/groups")
    public String adminGroupsPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        applyCommonAttributes(principal, model, "groups");
        return "admin-groups";
    }

    private void applyCommonAttributes(Principal principal, Model model, String currentPage) {
        UserProfileResponse profile = userService.getProfile(principal.getName());
        model.addAttribute("currentUser", profile.getUsername());
        model.addAttribute("currentTheme", profile.getTheme());
        model.addAttribute("currentPage", currentPage);
    }
}