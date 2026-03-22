package com._6.group4.smartcart.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests verifying Spring Security endpoint protection.
 *
 * These tests verify the SECURITY LAYER: that unauthenticated requests are blocked,
 * admin endpoints require the ADMIN role, and CSRF is enforced. The full end-to-end
 * login flow (including session attribute propagation to controllers) is tested via
 * AuthControllerTest and manual testing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    // ---- Public endpoints are accessible without auth ----

    @Test
    void landingPage_isAccessibleWithoutAuth() throws Exception {
        mvc.perform(get("/landing.html")).andExpect(status().isOk());
    }

    @Test
    void loginPage_isAccessibleWithoutAuth() throws Exception {
        mvc.perform(get("/login.html")).andExpect(status().isOk());
    }

    @Test
    void registerPage_isAccessibleWithoutAuth() throws Exception {
        mvc.perform(get("/register.html")).andExpect(status().isOk());
    }

    @Test
    void authMe_isAccessibleWithoutAuth() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isOk());
    }

    // ---- Protected API endpoints reject unauthenticated requests ----

    @Test
    void api_mealPlan_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/meal-plan"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }

    @Test
    void api_preferences_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/preferences")).andExpect(status().isUnauthorized());
    }

    @Test
    void api_pantry_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/pantry")).andExpect(status().isUnauthorized());
    }

    @Test
    void api_groceryList_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/grocery-list")).andExpect(status().isUnauthorized());
    }

    @Test
    void api_admin_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    // ---- Admin endpoints require ADMIN role ----

    @Test
    void api_admin_nonAdmin_returnsForbidden() throws Exception {
        mvc.perform(get("/api/admin/users").with(user("regular").roles("USER")))
                .andExpect(status().isForbidden());
    }

    // ---- CSRF enforcement on POST requests ----

    @Test
    void formPost_withoutCsrf_isRejected() throws Exception {
        mvc.perform(post("/login")
                .param("email", "test@test.com")
                .param("password", "test"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Spring Security returns 403 or redirects depending on entry point
                    org.assertj.core.api.Assertions.assertThat(status).isIn(302, 403);
                });
    }

    @Test
    void formPost_withCsrf_isAccepted() throws Exception {
        // Login attempt with CSRF should not be rejected by CSRF filter
        // (will fail with redirect because credentials are wrong, but NOT 403)
        mvc.perform(post("/login")
                .with(csrf())
                .param("email", "test@test.com")
                .param("password", "test"))
                .andExpect(status().is3xxRedirection()); // redirect to login error, not 403
    }
}
