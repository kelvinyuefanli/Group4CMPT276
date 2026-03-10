package com._6.group4.smartcart.admin;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private static final String SESSION_IS_ADMIN = "IS_ADMIN";

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session) {

        Boolean isAdmin = (Boolean) session.getAttribute(SESSION_IS_ADMIN);

        if (isAdmin == null || !isAdmin) {
            return "redirect:/";
        }

        return "admin.html";
    }
}