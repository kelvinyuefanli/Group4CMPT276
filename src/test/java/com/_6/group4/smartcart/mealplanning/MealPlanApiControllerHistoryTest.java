package com._6.group4.smartcart.mealplanning;
import com._6.group4.smartcart.auth.SessionKeys;
import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.auth.UserPreferencesRepository;
import com._6.group4.smartcart.auth.UserRepository;
import com._6.group4.smartcart.grocery.GroceryAggregationService;
import com._6.group4.smartcart.grocery.PantryItemRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MealPlanApiControllerHistoryTest {
    private MealPlanRepository mealPlanRepository;
    private MealPlanApiController controller;
    @BeforeEach
    void setUp() {
        mealPlanRepository = mock(MealPlanRepository.class);
        controller = new MealPlanApiController(
                mock(GeminiService.class), mock(RecipeRepository.class),
                mealPlanRepository, mock(UserRepository.class),
                mock(UserPreferencesRepository.class), mock(PantryItemRepository.class),
                new GroceryAggregationService(), mock(FavouriteRecipeRepository.class));
    }
    @Test @SuppressWarnings("unchecked")
    void getMealPlanHistory_returnsPaginatedResults() {
        Long userId = 42L;
        MealPlan plan1 = mealPlan(LocalDate.of(2026, 3, 30), slot(DayOfWeek.MONDAY, MealType.DINNER, "Pasta"), slot(DayOfWeek.TUESDAY, MealType.LUNCH, "Salad"));
        MealPlan plan2 = mealPlan(LocalDate.of(2026, 3, 23), slot(DayOfWeek.WEDNESDAY, MealType.BREAKFAST, "Omelette"));
        Page<MealPlan> page = new PageImpl<>(List.of(plan1, plan2), PageRequest.of(0, 5), 2);
        when(mealPlanRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        ResponseEntity<?> response = controller.getMealPlanHistory(0, 5, session(userId));
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals(2, items.size());
        assertEquals("2026-03-30", items.get(0).get("weekStartDate"));
        assertEquals(2, items.get(0).get("mealCount"));
        assertEquals(0, body.get("page"));
        assertEquals(2L, body.get("totalItems"));
    }
    @Test void getMealPlanHistory_requiresAuth() {
        assertEquals(401, controller.getMealPlanHistory(0, 5, new MockHttpSession()).getStatusCode().value());
    }
    @Test @SuppressWarnings("unchecked")
    void getMealPlanById_returnsFullMealData() {
        Long userId = 42L;
        MealPlan plan = mealPlan(LocalDate.of(2026, 3, 30), slot(DayOfWeek.MONDAY, MealType.DINNER, "Chicken Curry"), slot(DayOfWeek.TUESDAY, MealType.LUNCH, "Caesar Salad"));
        when(mealPlanRepository.findByIdAndUserId(10L, userId)).thenReturn(Optional.of(plan));
        ResponseEntity<?> response = controller.getMealPlanById(10L, session(userId));
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> meals = (List<Map<String, Object>>) body.get("meals");
        assertEquals(2, meals.size());
        assertTrue(meals.stream().anyMatch(m -> "Chicken Curry".equals(m.get("recipeName"))));
    }
    @Test void getMealPlanById_returns404ForWrongUser() {
        when(mealPlanRepository.findByIdAndUserId(99L, 42L)).thenReturn(Optional.empty());
        assertEquals(404, controller.getMealPlanById(99L, session(42L)).getStatusCode().value());
    }
    @Test void getMealPlanById_requiresAuth() {
        assertEquals(401, controller.getMealPlanById(10L, new MockHttpSession()).getStatusCode().value());
    }
    private HttpSession session(Long userId) { MockHttpSession s = new MockHttpSession(); s.setAttribute(SessionKeys.USER_ID, userId); return s; }
    private MealPlan mealPlan(LocalDate weekStart, MealPlanRecipe... slots) {
        MealPlan plan = new MealPlan(new User("h@e.com", "pw", "H"), weekStart); plan.onCreate();
        for (MealPlanRecipe slot : slots) { slot.setMealPlan(plan); plan.getRecipes().add(slot); }
        return plan;
    }
    private MealPlanRecipe slot(DayOfWeek d, MealType m, String t) { return new MealPlanRecipe(null, new Recipe(t), d, m); }
}
