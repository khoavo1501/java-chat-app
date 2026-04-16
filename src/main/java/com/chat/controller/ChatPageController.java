package com.chat.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatPageController {

    @GetMapping({ "/", "/chat" })
    public String chatPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        model.addAttribute("currentUser", principal.getName());
        return "chat";
    }
}
