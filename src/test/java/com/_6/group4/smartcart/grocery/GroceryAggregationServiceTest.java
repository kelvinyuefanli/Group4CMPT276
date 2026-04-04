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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        Map<String, GroceryAggregationService.GroceryListItemView> itemsByCanonicalName = response.items().stream()
                .collect(Collectors.toMap(GroceryAggregationService.GroceryListItemView::canonicalName, item -> item));

        assertEquals(2, response.items().size());

        GroceryAggregationService.GroceryListItemView chickpea = itemsByCanonicalName.get("chickpea");
        assertEquals("Garbanzo Beans", chickpea.name());
        assertEquals("1.5 lb", chickpea.quantity());
        assertEquals(1.5d, chickpea.quantityValue(), 0.0001d);
        assertEquals("lb", chickpea.unit());
        assertEquals("number", chickpea.inputMode());

        GroceryAggregationService.GroceryListItemView oliveOil = itemsByCanonicalName.get("olive oil");
        assertEquals("1.125 cup", oliveOil.quantity());
        assertEquals(1.125d, oliveOil.quantityValue(), 0.001d);
        assertEquals("cup", oliveOil.unit());

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
        assertTrue(quantities.contains("1 bunch"));
        assertTrue(quantities.contains("2 cup"));
    }

    @Test
    void buildGroceryList_subtractsPartialPantryAmountWithSameUnit() {
        MealPlan plan = mealPlan(recipe("Rice Bowl", spec("Rice", 3.0, "cup", null)));

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(
                plan,
                List.of(pantryItem("Rice", 1.0, "cup"))
        );

        GroceryAggregationService.GroceryListItemView rice = response.items().get(0);
        assertEquals(1, response.items().size());
        assertEquals("2 cup", rice.quantity());
        assertEquals(2.0d, rice.quantityValue(), 0.0001d);
        assertEquals(1.0d, rice.pantryQuantityValue(), 0.0001d);
        assertFalse(rice.covered());
        assertEquals(1, response.pantrySubtractedCount());
    }

    @Test
    void buildGroceryList_subtractsPartialPantryAmountAcrossConvertibleUnits() {
        MealPlan plan = mealPlan(recipe("Dressing", spec("Olive Oil", 1.0, "cup", null)));

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(
                plan,
                List.of(pantryItem("Olive Oil", 8.0, "tablespoons"))
        );

        GroceryAggregationService.GroceryListItemView oliveOil = response.items().get(0);
        assertEquals("0.5 cup", oliveOil.quantity());
        assertEquals(0.5d, oliveOil.quantityValue(), 0.001d);
        assertEquals(0.5d, oliveOil.pantryQuantityValue(), 0.001d);
        assertFalse(oliveOil.covered());
    }

    @Test
    void buildGroceryList_movesFullyCoveredNumericItemsIntoCoveredSection() {
        MealPlan plan = mealPlan(recipe("Bread", spec("Flour", 12.0, "oz", null)));

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(
                plan,
                List.of(pantryItem("Flour", 1.0, "lb"))
        );

        assertTrue(response.items().isEmpty());
        assertEquals(1, response.coveredItems().size());
        GroceryAggregationService.GroceryListItemView flour = response.coveredItems().get(0);
        assertEquals("0 oz", flour.quantity());
        assertEquals(0d, flour.quantityValue(), 0.0001d);
        assertEquals(16d, flour.pantryQuantityValue(), 0.0001d);
        assertTrue(flour.covered());
        assertTrue(response.allCoveredByPantry());
    }

    @Test
    void buildGroceryList_ignoresIncompatiblePantryUnits() {
        MealPlan plan = mealPlan(recipe("Soup", spec("Green Onion", 1.0, "bunch", null)));

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(
                plan,
                List.of(pantryItem("Green Onion", 1.0, "cup"))
        );

        GroceryAggregationService.GroceryListItemView greenOnion = response.items().get(0);
        assertEquals(1, response.items().size());
        assertEquals("1 bunch", greenOnion.quantity());
        assertNull(greenOnion.pantryQuantityValue());
        assertEquals(0, response.pantrySubtractedCount());
    }

    @Test
    void buildGroceryList_usesBooleanPantryCoverageForUnknownQuantities() {
        MealPlan plan = mealPlan(recipe("Noodles", spec("Soy Sauce", null, "tbsp", null)));

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(
                plan,
                List.of(pantryItem("Soy Sauce"))
        );

        assertTrue(response.items().isEmpty());
        assertEquals(1, response.coveredItems().size());
        GroceryAggregationService.GroceryListItemView soySauce = response.coveredItems().get(0);
        assertEquals("toggle", soySauce.inputMode());
        assertEquals("tbsp", soySauce.quantity());
        assertTrue(soySauce.covered());
    }

    @Test
    void buildGroceryList_usesLegacyBooleanPantryRowsToCoverNumericItems() {
        MealPlan plan = mealPlan(recipe("Breakfast", spec("Spring onions", 1.0, "bunch", "green onion")));

        GroceryAggregationService.GroceryListResponse response = service.buildGroceryList(
                plan,
                List.of(pantryItem("scallions"))
        );

        assertTrue(response.items().isEmpty());
        assertEquals(1, response.coveredItems().size());
        GroceryAggregationService.GroceryListItemView greenOnion = response.coveredItems().get(0);
        assertEquals("0 bunch", greenOnion.quantity());
        assertEquals(0d, greenOnion.quantityValue(), 0.0001d);
        assertNull(greenOnion.pantryQuantityValue());
        assertTrue(greenOnion.covered());
        assertEquals(1, response.pantrySubtractedCount());
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

    private PantryItem pantryItem(String name, Double quantity, String unit) {
        PantryItem pantryItem = pantryItem(name);
        pantryItem.setQuantity(quantity);
        pantryItem.setUnit(GroceryAggregationService.normalizeStoredUnit(unit));
        return pantryItem;
    }

    private IngredientSpec spec(String name, Double quantity, String unit, String canonicalName) {
        return new IngredientSpec(name, quantity, unit, canonicalName);
    }

    private record IngredientSpec(String name, Double quantity, String unit, String canonicalName) {
    }
}
