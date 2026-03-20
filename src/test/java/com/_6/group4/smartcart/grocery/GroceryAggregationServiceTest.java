package com._6.group4.smartcart.grocery;

import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.mealplanning.DayOfWeek;
import com._6.group4.smartcart.mealplanning.MealPlan;
import com._6.group4.smartcart.mealplanning.MealPlanRecipe;
import com._6.group4.smartcart.mealplanning.MealType;
import com._6.group4.smartcart.mealplanning.Recipe;
import com._6.group4.smartcart.mealplanning.RecipeIngredient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroceryAggregationServiceTest {

    private final GroceryAggregationService service = new GroceryAggregationService();

    @Test
    void buildGroceryList_mergesAliasesAndConvertibleUnits() {
        MealPlan plan = mealPlan(
                recipe(
                        "Mediterranean Bowl",
                        spec("Garbanzo Beans", 1.0, "lb", null),
                        spec("Olive Oil", 1.0, "cup", null)
                ),
                recipe(
                        "Chickpea Salad",
                        spec("chickpeas", 8.0, "oz", null),
                        spec("olive oil", 2.0, "tablespoons", null)
                )
        );

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(plan, List.of());

        Map<String, String> quantitiesByName = response.items().stream()
                .collect(java.util.stream.Collectors.toMap(
                        GroceryAggregationService.GroceryListItemView::name,
                        GroceryAggregationService.GroceryListItemView::quantity
                ));

        assertEquals(2, response.items().size());
        assertEquals("1.5 lb", quantitiesByName.get("Garbanzo Beans"));
        assertEquals("1.125 cup", quantitiesByName.get("Olive Oil"));
        assertEquals(0, response.pantrySubtractedCount());
        assertFalse(response.allCoveredByPantry());
    }

    @Test
    void buildGroceryList_keepsIncompatibleUnitsSeparate() {
        MealPlan plan = mealPlan(
                recipe("Soup", spec("Green Onion", 1.0, "bunch", null)),
                recipe("Salad", spec("Green Onion", 2.0, "cup", null))
        );

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(plan, List.of());

        List<String> quantities = response.items().stream()
                .map(GroceryAggregationService.GroceryListItemView::quantity)
                .toList();

        assertEquals(2, response.items().size());
        assertIterableEquals(List.of("1 bunch", "2 cup"), quantities);
    }

    @Test
    void buildGroceryList_keepsUnknownQuantitiesSeparateFromKnownOnes() {
        MealPlan plan = mealPlan(
                recipe("Stir Fry", spec("Soy Sauce", null, "tbsp", null)),
                recipe("Noodles", spec("Soy Sauce", 2.0, "tbsp", null))
        );

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(plan, List.of());

        List<String> quantities = response.items().stream()
                .map(GroceryAggregationService.GroceryListItemView::quantity)
                .toList();

        assertEquals(2, response.items().size());
        assertTrue(quantities.contains("tbsp"));
        assertTrue(quantities.contains("2 tbsp"));
    }

    @Test
    void buildGroceryList_subtractsPantryItemsByCanonicalName() {
        MealPlan plan = mealPlan(
                recipe("Tacos", spec("Scallions", 2.0, "bunch", null)),
                recipe("Rice Bowl", spec("Rice", 1.0, "cup", null))
        );

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(
                plan,
                List.of(pantryItem("green onion"))
        );

        assertEquals(1, response.items().size());
        assertEquals("Rice", response.items().get(0).name());
        assertEquals(1, response.pantrySubtractedCount());
        assertFalse(response.allCoveredByPantry());
    }

    @Test
    void buildGroceryList_marksWhenPantryCoversEverything() {
        MealPlan plan = mealPlan(
                recipe("Breakfast", spec("Spring onions", 1.0, "bunch", "green onion"))
        );

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(
                plan,
                List.of(pantryItem("scallions"))
        );

        assertTrue(response.items().isEmpty());
        assertEquals(1, response.pantrySubtractedCount());
        assertTrue(response.allCoveredByPantry());
        assertEquals(Boolean.TRUE, response.toResponseMap().get("allCoveredByPantry"));
    }

    private MealPlan mealPlan(Recipe... recipes) {
        User user = new User("test@example.com", "pw", "Test User");
        MealPlan plan = new MealPlan(user, LocalDate.of(2026, 3, 16));
        DayOfWeek[] days = DayOfWeek.values();
        for (int i = 0; i < recipes.length; i++) {
            plan.getRecipes().add(new MealPlanRecipe(plan, recipes[i], days[i], MealType.DINNER));
        }
        return plan;
    }

    private Recipe recipe(String title, IngredientSpec... specs) {
        Recipe recipe = new Recipe(title);
        for (IngredientSpec spec : specs) {
            RecipeIngredient ingredient = new RecipeIngredient(recipe, spec.name);
            ingredient.setQuantity(spec.quantity);
            ingredient.setUnit(spec.unit);
            ingredient.setCanonicalName(spec.canonicalName);
            recipe.getIngredients().add(ingredient);
        }
        return recipe;
    }

    private PantryItem pantryItem(String name) {
        return new PantryItem(new User("pantry@example.com", "pw", "Pantry User"), name);
    }

    private IngredientSpec spec(String name, Double quantity, String unit, String canonicalName) {
        return new IngredientSpec(name, quantity, unit, canonicalName);
    }

    private record IngredientSpec(String name, Double quantity, String unit, String canonicalName) {
    }
}
