package com._6.group4.smartcart.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.auth.UserRepository;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final UserRepository userRepository;

    public AdminApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<User> getAllUsers(HttpSession session) {

        Boolean isAdmin = (Boolean) session.getAttribute("IS_ADMIN");

        if (isAdmin == null || !isAdmin) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findAll();
    }
}