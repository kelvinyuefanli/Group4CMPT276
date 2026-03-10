package com._6.group4.smartcart.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com._6.group4.smartcart.auth.User;

import java.util.Map;

@Controller
public class AuthController {

    private static final String SESSION_USER_ID = "USER_ID";
    private static final String SESSION_USER_EMAIL = "USER_EMAIL";
    private static final String SESSION_IS_ADMIN = "IS_ADMIN";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Returns current session auth state for the main UI header. */
    @GetMapping(value = "/api/auth/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> authMe(HttpSession session) {
        Object userId = session.getAttribute(SESSION_USER_ID);
        Object email = session.getAttribute(SESSION_USER_EMAIL);
        boolean loggedIn = userId != null && email != null;
        Map<String, Object> body = loggedIn
            ? Map.of("loggedIn", true, "email", email.toString())
            : Map.of("loggedIn", false);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        if (session.getAttribute(SESSION_USER_ID) == null) {
            return "redirect:/login.html";
        }
        return "redirect:/";
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "redirect:/register.html";
    }

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam String password,
            @RequestParam String confirmPassword
    ) {
        try {
            authService.register(email, password, confirmPassword, name);
            return "redirect:/login.html?registered=1";
        } catch (IllegalArgumentException ex) {
            String params = "?error=" + java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8)
                + "&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)
                + "&name=" + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/register.html" + params;
        }
    }

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "logout", required = false) String loggedOut
    ) {
        if (registered != null) {
            return "redirect:/login.html?registered=1";
        }
        if (loggedOut != null) {
            return "redirect:/login.html?logout=1";
        }
        return "redirect:/login.html";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session
    ) {
        try {
            User user = authService.login(email, password);
            session.setAttribute(SESSION_USER_ID, user.getId());
            session.setAttribute(SESSION_USER_EMAIL, user.getEmail());
            session.setAttribute(SESSION_IS_ADMIN, user.isAdmin());
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            String params = "?error=" + java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8)
                + "&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/login.html" + params;
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login.html?logout=1";
    }
}

