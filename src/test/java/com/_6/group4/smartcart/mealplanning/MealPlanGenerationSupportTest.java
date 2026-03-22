package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.mealplanning.dto.GeminiMealPlanDto;
import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MealPlanGenerationSupportTest {

    @Test
    void normalizesSelectionsAndServingSize() {
        assertThat(MealPlanGenerationSupport.normalizeSelectionList("Vegan, Gluten-Free, vegan,  Gluten-Free "))
            .isEqualTo("Vegan, Gluten-Free");
        assertThat(MealPlanGenerationSupport.normalizeServingSize(null, null)).isEqualTo(2);
        assertThat(MealPlanGenerationSupport.normalizeServingSize(0, 4)).isEqualTo(1);
        assertThat(MealPlanGenerationSupport.normalizeServingSize(8, 4)).isEqualTo(6);
    }

    @Test
    void parsesExpectedMealSlotsFromScheduleJson() {
        Set<MealPlanGenerationSupport.MealSlot> slots = MealPlanGenerationSupport.expectedSlots("""
            {
              "MONDAY": ["BREAKFAST", "DINNER"],
              "FRIDAY": ["LUNCH"]
            }
            """);

        assertThat(slots).containsExactly(
            new MealPlanGenerationSupport.MealSlot(DayOfWeek.MONDAY, MealType.BREAKFAST),
            new MealPlanGenerationSupport.MealSlot(DayOfWeek.MONDAY, MealType.DINNER),
            new MealPlanGenerationSupport.MealSlot(DayOfWeek.FRIDAY, MealType.LUNCH)
        );
    }

    @Test
    void extractsOnlyValidRequestedMealsAndDefaultsServings() {
        GeminiRecipeDto validRecipe = new GeminiRecipeDto(
            "Tofu Bowl",
            "Korean",
            20,
            null,
            "Cook rice.\nRoast tofu.",
            List.of(new GeminiRecipeDto.IngredientDto("tofu", "1", "block"))
        );
        GeminiRecipeDto invalidRecipe = new GeminiRecipeDto(
            "Untitled",
            "Korean",
            20,
            2,
            null,
            List.of()
        );

        GeminiMealPlanDto dto = new GeminiMealPlanDto(List.of(
            new GeminiMealPlanDto.MealEntry("MONDAY", "BREAKFAST", validRecipe),
            new GeminiMealPlanDto.MealEntry("MONDAY", "BREAKFAST", invalidRecipe),
            new GeminiMealPlanDto.MealEntry("MONDAY", "DINNER", invalidRecipe),
            new GeminiMealPlanDto.MealEntry("SOMEDAY", "LUNCH", validRecipe)
        ));

        Set<MealPlanGenerationSupport.MealSlot> requestedSlots = MealPlanGenerationSupport.expectedSlots("""
            { "MONDAY": ["BREAKFAST", "DINNER"] }
            """);

        MealPlanGenerationSupport.ExtractedMeals extracted =
            MealPlanGenerationSupport.extractValidMeals(dto, requestedSlots, 3);

        assertThat(extracted.acceptedMeals()).hasSize(1);
        assertThat(extracted.missingSlots()).containsExactly(
            new MealPlanGenerationSupport.MealSlot(DayOfWeek.MONDAY, MealType.DINNER)
        );

        GeminiMealPlanDto.MealEntry acceptedEntry = extracted.acceptedMeals().values().iterator().next();
        assertThat(acceptedEntry.recipe().servings()).isEqualTo(3);
    }
}
