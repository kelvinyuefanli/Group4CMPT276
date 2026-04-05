package com._6.group4.smartcart.instacart;

import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.grocery.GroceryAggregationService;
import com._6.group4.smartcart.grocery.PantryItem;
import com._6.group4.smartcart.grocery.PantryItemRepository;
import com._6.group4.smartcart.mealplanning.DayOfWeek;
import com._6.group4.smartcart.mealplanning.MealPlan;
import com._6.group4.smartcart.mealplanning.MealPlanRecipe;
import com._6.group4.smartcart.mealplanning.MealPlanRepository;
import com._6.group4.smartcart.mealplanning.MealType;
import com._6.group4.smartcart.mealplanning.Recipe;
import com._6.group4.smartcart.mealplanning.RecipeIngredient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstacartShoppingListServiceTest {

    private MealPlanRepository mealPlanRepository;
    private PantryItemRepository pantryItemRepository;
    private InstacartGateway instacartGateway;
    private InstacartShoppingListService service;

    @BeforeEach
    void setUp() {
        mealPlanRepository = mock(MealPlanRepository.class);
        pantryItemRepository = mock(PantryItemRepository.class);
        instacartGateway = mock(InstacartGateway.class);
        service = new InstacartShoppingListService(
                mealPlanRepository,
                pantryItemRepository,
                new GroceryAggregationService(),
                instacartGateway,
                "https://smartcart.example/grocery",
                14
        );
    }

    @Test
    void createShoppingListLinkForUser_usesRemainingPantryAdjustedItems() {
        Long userId = 42L;
        MealPlan plan = mealPlan(
                recipe("Dinner", ingredient("Olive Oil", 1.0, "cup"), ingredient("Rice", 2.0, "cup"))
        );
        PantryItem oliveOil = pantryItem("Olive Oil", 1.0, "cup");

        when(mealPlanRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(plan));
        when(pantryItemRepository.findAllByUserIdOrderByIngredientName(userId)).thenReturn(List.of(oliveOil));
        when(instacartGateway.createProductsLink(any())).thenAnswer(invocation -> {
            InstacartShoppingListService.ProductsLinkRequest request = invocation.getArgument(0);
            assertEquals("SmartCart Grocery List - Week of 2026-03-16", request.title());
            assertEquals("shopping_list", request.linkType());
            assertEquals(14, request.expiresIn());
            assertEquals(1, request.lineItems().size());
            InstacartShoppingListService.LineItem lineItem = request.lineItems().get(0);
            assertEquals("Rice", lineItem.name());
            assertEquals(2.0d, lineItem.quantity());
            assertEquals("cup", lineItem.unit());
            assertEquals("2 cup Rice", lineItem.displayText());
            return new InstacartShoppingListService.ProductsLinkResponse("https://instacart.example/list");
        });

        InstacartShoppingListService.ShoppingListLinkResult result =
                service.createShoppingListLinkForUser(userId);

        assertEquals("https://instacart.example/list", result.productsLinkUrl());
        assertEquals(1, result.itemCount());
    }

    @Test
    void createShoppingListLinkForUser_dropsUnsupportedUnitsInsteadOfSendingBadMeasurements() {
        Long userId = 42L;
        MealPlan plan = mealPlan(recipe("Dinner", ingredient("Garlic", 3.0, "cloves")));

        when(mealPlanRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(plan));
        when(pantryItemRepository.findAllByUserIdOrderByIngredientName(userId)).thenReturn(List.of());
        when(instacartGateway.createProductsLink(any())).thenAnswer(invocation -> {
            InstacartShoppingListService.ProductsLinkRequest request = invocation.getArgument(0);
            InstacartShoppingListService.LineItem lineItem = request.lineItems().get(0);
            assertEquals("Garlic", lineItem.name());
            assertNull(lineItem.quantity());
            assertNull(lineItem.unit());
            assertTrue(lineItem.displayText().contains("Garlic"));
            assertTrue(lineItem.displayText().startsWith("3 "));
            return new InstacartShoppingListService.ProductsLinkResponse("https://instacart.example/list");
        });

        service.createShoppingListLinkForUser(userId);
    }

    @Test
    void createShoppingListLinkForUser_rejectsWhenNothingRemainsToBuy() {
        Long userId = 42L;
        MealPlan plan = mealPlan(recipe("Dinner", ingredient("Olive Oil", 1.0, "cup")));
        PantryItem oliveOil = pantryItem("Olive Oil", 1.0, "cup");

        when(mealPlanRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(plan));
        when(pantryItemRepository.findAllByUserIdOrderByIngredientName(userId)).thenReturn(List.of(oliveOil));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createShoppingListLinkForUser(userId)
        );

        assertEquals("There are no remaining grocery items to send to Instacart.", ex.getMessage());
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
        pantryItem.setQuantity(quantity);
        pantryItem.setUnit(unit);
        return pantryItem;
    }
}
