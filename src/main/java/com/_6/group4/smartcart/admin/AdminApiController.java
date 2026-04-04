package com._6.group4.smartcart.admin;

import com._6.group4.smartcart.auth.SessionKeys;
import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.auth.UserRepository;
import com._6.group4.smartcart.mealplanning.MealPlanRepository;
import com._6.group4.smartcart.mealplanning.RecipeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {


    private final UserRepository userRepository;
    private final MealPlanRepository mealPlanRepository;
    private final RecipeRepository recipeRepository;

    public AdminApiController(UserRepository userRepository,
                              MealPlanRepository mealPlanRepository,
                              RecipeRepository recipeRepository) {
        this.userRepository = userRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.recipeRepository = recipeRepository;
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(SessionKeys.IS_ADMIN));
    }

    private static final ResponseEntity<?> FORBIDDEN =
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(HttpSession session) {
        if (!isAdmin(session)) return FORBIDDEN;
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(AdminApiController::toUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return FORBIDDEN;

        Long currentUserId = getSessionUserId(session);
        if (currentUserId != null && currentUserId.equals(id)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot delete your own account"));
        }

        return userRepository.findById(id)
                .map(user -> {
                    if (user.isAdmin()) {
                        return ResponseEntity.badRequest()
                                .body((Object) Map.of("error", "Cannot delete another admin"));
                    }
                    userRepository.delete(user);
                    return ResponseEntity.ok((Object) Map.of("status", "deleted"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(HttpSession session) {
        if (!isAdmin(session)) return FORBIDDEN;
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalMealPlans", mealPlanRepository.count());
        stats.put("totalRecipes", recipeRepository.count());
        return ResponseEntity.ok(stats);
    }

    private Long getSessionUserId(HttpSession session) {
        Object id = session.getAttribute(SessionKeys.USER_ID);
        if (id instanceof Long) return (Long) id;
        if (id instanceof Number) return ((Number) id).longValue();
        return null;
    }

    private static Map<String, Object> toUserDto(User user) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", user.getId());
        dto.put("name", user.getName());
        dto.put("email", user.getEmail());
        dto.put("admin", user.isAdmin());
        dto.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        return dto;
    }
}
