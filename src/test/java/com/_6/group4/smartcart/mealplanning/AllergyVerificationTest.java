package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
                List.of(Map.of("name", "chicken breast")));
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of())).isFalse();
    }

    @Test
    void recipeContainsAllergen_noIngredientsReturnsFalse() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "Test", "Italian", 30, 4, "Instructions", null);
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of("peanuts"))).isFalse();
    }

    @Test
    void recipeContainsAllergen_detectsAllergenInMapIngredient() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "Pad Thai", "Thai", 25, 2, "Instructions",
                List.of(
                        Map.of("name", "rice noodles", "quantity", 200, "unit", "g"),
                        Map.of("name", "peanut butter", "quantity", 2, "unit", "tbsp")
                ));
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of("peanut"))).isTrue();
    }

    @Test
    void recipeContainsAllergen_detectsAllergenInStringIngredient() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "PB Sandwich", null, 5, 1, "Spread",
                List.of("peanut butter", "bread"));
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of("peanut"))).isTrue();
    }

    @Test
    void recipeContainsAllergen_safeRecipeReturnsFalse() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "Grilled Chicken", "American", 20, 2, "Grill it",
                List.of(
                        Map.of("name", "chicken breast"),
                        Map.of("name", "olive oil"),
                        Map.of("name", "salt")
                ));
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of("peanut", "shellfish"))).isFalse();
    }

    @Test
    void recipeContainsAllergen_caseInsensitive() {
        GeminiRecipeDto recipe = new GeminiRecipeDto(
                "Test", null, 10, 1, "Test",
                List.of(Map.of("name", "Shrimp Tempura")));
        assertThat(MealPlanApiController.recipeContainsAllergen(recipe, Set.of("shrimp"))).isTrue();
    }
}
