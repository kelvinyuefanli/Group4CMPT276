package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto.IngredientDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for allergy verification logic in MealPlanApiController.
 */
class AllergyVerificationTest {

    // ---- parseAllergenSet ----

    @Test
    void parseAllergenSet_nullReturnsEmpty() {
        assertThat(MealPlanApiController.parseAllergenSet(null)).isEmpty();
    }

    @Test
    void parseAllergenSet_blankReturnsEmpty() {
        assertThat(MealPlanApiController.parseAllergenSet("")).isEmpty();
        assertThat(MealPlanApiController.parseAllergenSet("   ")).isEmpty();
    }

    @Test
    void parseAllergenSet_parsesCommaSeparated() {
        Set<String> result = MealPlanApiController.parseAllergenSet("Peanuts, Tree Nuts, Shellfish");
        assertThat(result).containsExactlyInAnyOrder("peanuts", "tree nuts", "shellfish");
    }

    @Test
    void parseAllergenSet_handlesSemicolons() {
        Set<String> result = MealPlanApiController.parseAllergenSet("Peanuts; Soy");
        assertThat(result).containsExactlyInAnyOrder("peanuts", "soy");
    }

    @Test
    void parseAllergenSet_lowercases() {
        Set<String> result = MealPlanApiController.parseAllergenSet("PEANUTS");
        assertThat(result).contains("peanuts");
    }

    // ---- recipeContainsAllergen ----

    @Test
    void recipeContainsAllergen_noAllergensReturnsFalse() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "Test", "Italian", 30, 4, "Instructions",
                List.of(new IngredientDto("chicken breast", 1.0, "lb")));
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of())).isFalse();
    }

    @Test
    void recipeContainsAllergen_noIngredientsReturnsFalse() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "Test", "Italian", 30, 4, "Instructions", null);
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of("peanuts"))).isFalse();
    }

    @Test
    void recipeContainsAllergen_detectsAllergenInIngredient() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "Pad Thai", "Thai", 25, 2, "Instructions",
                List.of(
                        new IngredientDto("rice noodles", 200, "g"),
                        new IngredientDto("peanut butter", 2, "tbsp")
                ));
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of("peanut"))).isTrue();
    }

    @Test
    void recipeContainsAllergen_safeRecipeReturnsFalse() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "Grilled Chicken", "American", 20, 2, "Grill it",
                List.of(
                        new IngredientDto("chicken breast", 1, "lb"),
                        new IngredientDto("olive oil", 2, "tbsp"),
                        new IngredientDto("salt", 1, "tsp")
                ));
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of("peanut", "shellfish"))).isFalse();
    }

    @Test
    void recipeContainsAllergen_caseInsensitive() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "Test", null, 10, 1, "Test",
                List.of(new IngredientDto("Shrimp Tempura", 1, "serving")));
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of("shrimp"))).isTrue();
    }
}
