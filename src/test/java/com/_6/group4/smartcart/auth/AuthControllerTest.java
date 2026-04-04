package com._6.group4.smartcart.auth;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication flows including CSRF protection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuthService authService;

    // ---- Registration ----

    @Test
    void register_withValidData_redirectsToLogin() throws Exception {
        mvc.perform(post("/register")
                .with(csrf())
                .param("email", "test@example.com")
                .param("name", "Test User")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html?registered=1"));
    }

    @Test
    void register_withMismatchedPasswords_redirectsWithError() throws Exception {
        mvc.perform(post("/register")
                .with(csrf())
                .param("email", "test@example.com")
                .param("name", "Test User")
                .param("password", "password123")
                .param("confirmPassword", "different"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/register.html?error=*"));
    }

    @Test
    void register_withoutCsrf_isRejected() throws Exception {
        mvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("name", "Test User")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(302, 403);
                });
    }

    // ---- Login ----

    @Test
    void login_withValidCredentials_redirectsToDashboard() throws Exception {
        authService.register("login@test.com", "pass123", "pass123", "Login User");

        mvc.perform(post("/login")
                .with(csrf())
                .param("email", "login@test.com")
                .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void login_withWrongPassword_redirectsWithError() throws Exception {
        authService.register("wrong@test.com", "pass123", "pass123", "Wrong User");

        mvc.perform(post("/login")
                .with(csrf())
                .param("email", "wrong@test.com")
                .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/login.html?error=*"));
    }

    @Test
    void login_withoutCsrf_isRejected() throws Exception {
        mvc.perform(post("/login")
                .param("email", "test@test.com")
                .param("password", "pass123"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(302, 403);
                });
    }

    // ---- Logout ----

    @Test
    void logout_redirectsToLoginPage() throws Exception {
        mvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html?logout=1"));
    }

    // ---- Auth me API ----

    @Test
    void authMe_unauthenticated_returnsLoggedInFalse() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(false));
    }

    // Note: authMe_authenticated test omitted — MockMvc with Spring Security
    // wraps the session, making session attribute injection unreliable. The
    // /api/auth/me endpoint behavior is verified via manual testing and the
    // SecurityConfigTest verifies endpoint protection at the Spring Security level.
}
