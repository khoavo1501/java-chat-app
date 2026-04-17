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
    public String adminPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        UserProfileResponse profile = userService.getProfile(principal.getName());
        model.addAttribute("currentUser", profile.getUsername());
        model.addAttribute("currentTheme", profile.getTheme());
        return "admin";
    }
}