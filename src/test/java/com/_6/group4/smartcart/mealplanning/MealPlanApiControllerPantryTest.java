package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.auth.UserPreferencesRepository;
import com._6.group4.smartcart.auth.UserRepository;
import com._6.group4.smartcart.grocery.GroceryAggregationService;
import com._6.group4.smartcart.grocery.PantryItem;
import com._6.group4.smartcart.grocery.PantryItemRepository;
import com._6.group4.smartcart.grocery.PantryItemUpdateRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MealPlanApiControllerPantryTest {

    private MealPlanRepository mealPlanRepository;
    private UserRepository userRepository;
    private PantryItemRepository pantryItemRepository;
    private MealPlanApiController controller;

    @BeforeEach
    void setUp() {
        GeminiService geminiService = mock(GeminiService.class);
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        mealPlanRepository = mock(MealPlanRepository.class);
        userRepository = mock(UserRepository.class);
        UserPreferencesRepository preferencesRepository = mock(UserPreferencesRepository.class);
        pantryItemRepository = mock(PantryItemRepository.class);

        controller = new MealPlanApiController(
                geminiService,
                recipeRepository,
                mealPlanRepository,
                userRepository,
                preferencesRepository,
                pantryItemRepository,
                new GroceryAggregationService()
        );
    }

    @Test
    void updatePantryItem_savesNumericPantryAmountForInlineEntry() {
        Long userId = 42L;
        User user = new User("user@example.com", "pw", "User");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(pantryItemRepository.findFirstByUserIdAndCanonicalNameAndUnitAndQuantityIsNotNull(userId, "olive oil", "cup"))
                .thenReturn(Optional.empty());
        when(pantryItemRepository.save(any(PantryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.updatePantryItem(
                new PantryItemUpdateRequest("Olive Oil", "olive oil", 1.5, "cups", true),
                session(userId)
        );

        assertEquals(200, response.getStatusCode().value());
        verify(pantryItemRepository).deleteAllByUserIdAndCanonicalNameAndQuantityIsNull(userId, "olive oil");
        verify(pantryItemRepository).save(any(PantryItem.class));
        verifySavedNumericItem(user, 1.5, "cup");
    }

    @Test
    void updatePantryItem_clearingInlineAmountRemovesNumericPantryEntry() {
        Long userId = 42L;

        ResponseEntity<?> response = controller.updatePantryItem(
                new PantryItemUpdateRequest("Olive Oil", "olive oil", 0d, "cup", false),
                session(userId)
        );

        assertEquals(200, response.getStatusCode().value());
        verify(pantryItemRepository).deleteAllByUserIdAndCanonicalNameAndUnitAndQuantityIsNotNull(userId, "olive oil", "cup");
        verify(pantryItemRepository, never()).save(any(PantryItem.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getGroceryList_returnsRemainingAndCoveredItemsWithInlineMetadata() {
        Long userId = 42L;
        MealPlan plan = mealPlan(
                recipe("Dinner", ingredient("Olive Oil", 1.0, "cup")),
                recipe("Lunch", ingredient("Rice", 2.0, "cup"))
        );
        PantryItem oliveOil = pantryItem("Olive Oil", 1.0, "cup");

        when(mealPlanRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(plan));
        when(pantryItemRepository.findAllByUserIdOrderByIngredientName(userId)).thenReturn(List.of(oliveOil));

        ResponseEntity<?> response = controller.getGroceryList(session(userId));

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("items"));
        assertTrue(body.containsKey("coveredItems"));
        assertTrue(body.containsKey("pantrySubtractedCount"));
        assertTrue(body.containsKey("allCoveredByPantry"));

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        List<Map<String, Object>> coveredItems = (List<Map<String, Object>>) body.get("coveredItems");

        assertEquals(1, items.size());
        assertEquals(1, coveredItems.size());

        Map<String, Object> remainingRice = items.get(0);
        assertEquals("Rice", remainingRice.get("name"));
        assertEquals("2 cup", remainingRice.get("quantity"));
        assertEquals("number", remainingRice.get("inputMode"));
        assertFalse((Boolean) remainingRice.get("covered"));
        assertNotNull(remainingRice.get("itemKey"));
        assertNotNull(remainingRice.get("canonicalName"));
        assertNotNull(remainingRice.get("quantityValue"));
        assertNotNull(remainingRice.get("unit"));

        Map<String, Object> coveredOliveOil = coveredItems.get(0);
        assertEquals("Olive Oil", coveredOliveOil.get("name"));
        assertEquals("0 cup", coveredOliveOil.get("quantity"));
        assertEquals(1.0d, coveredOliveOil.get("pantryQuantityValue"));
        assertEquals(Boolean.TRUE, coveredOliveOil.get("covered"));
        assertEquals(Boolean.FALSE, body.get("allCoveredByPantry"));
        assertEquals(1, body.get("pantrySubtractedCount"));
    }

    private void verifySavedNumericItem(User expectedUser, Double expectedQuantity, String expectedUnit) {
        verify(pantryItemRepository).save(org.mockito.ArgumentMatchers.argThat(item ->
                item.getUser() == expectedUser
                        && "Olive Oil".equals(item.getIngredientName())
                        && "olive oil".equals(item.getCanonicalName())
                        && expectedQuantity.equals(item.getQuantity())
                        && expectedUnit.equals(item.getUnit())
        ));
    }

    private HttpSession session(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", userId);
        return session;
    }

    private MealPlan mealPlan(Recipe... recipes) {
        User user = new User("plan@example.com", "pw", "Planner");
        MealPlan plan = new MealPlan(user, LocalDate.of(2026, 3, 16));
        DayOfWeek[] days = DayOfWeek.values();
        for (int i = 0; i < recipes.length; i++) {
            plan.getRecipes().add(new MealPlanRecipe(plan, recipes[i], days[i], MealType.DINNER));
        }
        return plan;
    }

    private Recipe recipe(String title, RecipeIngredient... ingredients) {
        Recipe recipe = new Recipe(title);
        for (RecipeIngredient ingredient : ingredients) {
            ingredient.setRecipe(recipe);
            recipe.getIngredients().add(ingredient);
        }
        return recipe;
    }

    private RecipeIngredient ingredient(String name, Double quantity, String unit) {
        RecipeIngredient ingredient = new RecipeIngredient(null, name);
        ingredient.setQuantity(quantity);
        ingredient.setUnit(unit);
        return ingredient;
    }

    private PantryItem pantryItem(String name, Double quantity, String unit) {
        PantryItem pantryItem = new PantryItem(new User("pantry@example.com", "pw", "Pantry"), name);
        pantryItem.setCanonicalName("olive oil");
        pantryItem.setQuantity(quantity);
        pantryItem.setUnit(unit);
        return pantryItem;
    }
}
