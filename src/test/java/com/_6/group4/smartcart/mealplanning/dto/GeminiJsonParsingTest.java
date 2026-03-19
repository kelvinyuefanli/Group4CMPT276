package com._6.group4.smartcart.mealplanning.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiJsonParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesMixedIngredientFormats() throws Exception {
        String json = """
            {
              "title": "Overnight Oats",
              "cuisine": null,
              "cookTimeMinutes": 5,
              "servings": 1,
              "instructions": "Mix and chill overnight.",
              "ingredients": [
                "rolled oats",
                { "name": "milk", "quantity": "1 1/2", "unit": "cups" },
                { "name": "cinnamon", "quantity": "\\u00BD", "unit": "tsp" },
                { "name": "chia seeds", "quantity": "about 2", "unit": "tbsp" },
                { "name": "vanilla", "quantity": "a splash", "unit": null }
              ]
            }
            """;

        GeminiRecipeDto dto = mapper.readValue(json, GeminiRecipeDto.class);

        assertThat(dto.normalizedIngredients()).extracting(GeminiRecipeDto.IngredientDto::safeName)
            .containsExactly("rolled oats", "milk", "cinnamon", "chia seeds", "vanilla");
        assertThat(dto.normalizedIngredients().get(1).quantityAsDouble()).isEqualTo(1.5d);
        assertThat(dto.normalizedIngredients().get(2).quantityAsDouble()).isEqualTo(0.5d);
        assertThat(dto.normalizedIngredients().get(3).quantityAsDouble()).isEqualTo(2.0d);
        assertThat(dto.normalizedIngredients().get(4).quantityAsDouble()).isNull();
    }

    @Test
    void parsesArrayBackedGeminiFields() throws Exception {
        String mealPlanJson = """
            {
              "meals": [
                {
                  "dayOfWeek": ["MONDAY"],
                  "mealType": ["DINNER"],
                  "recipe": {
                    "title": ["Sheet Pan Tofu"],
                    "cuisine": ["Korean"],
                    "cookTimeMinutes": ["25"],
                    "servings": { "value": "2" },
                    "instructions": [
                      "Press the tofu.",
                      { "text": "Roast with vegetables until browned." }
                    ],
                    "ingredients": [
                      {
                        "name": ["firm tofu"],
                        "quantity": ["1", "1/2"],
                        "unit": ["blocks"]
                      },
                      {
                        "name": "gochujang",
                        "quantity": { "value": "2" },
                        "unit": ["tbsp"]
                      }
                    ]
                  }
                }
              ]
            }
            """;

        GeminiMealPlanDto plan = mapper.readValue(mealPlanJson, GeminiMealPlanDto.class);
        GeminiMealPlanDto.MealEntry entry = plan.meals().get(0);

        assertThat(entry.dayOfWeek()).isEqualTo("MONDAY");
        assertThat(entry.mealType()).isEqualTo("DINNER");
        assertThat(entry.recipe().title()).isEqualTo("Sheet Pan Tofu");
        assertThat(entry.recipe().cuisine()).isEqualTo("Korean");
        assertThat(entry.recipe().cookTimeMinutes()).isEqualTo(25);
        assertThat(entry.recipe().servings()).isEqualTo(2);
        assertThat(entry.recipe().instructions()).contains("Press the tofu.");
        assertThat(entry.recipe().instructions()).contains("Roast with vegetables until browned.");
        assertThat(entry.recipe().normalizedIngredients().get(0).safeName()).isEqualTo("firm tofu");
        assertThat(entry.recipe().normalizedIngredients().get(0).quantityAsDouble()).isEqualTo(1.5d);
        assertThat(entry.recipe().normalizedIngredients().get(0).safeUnit()).isEqualTo("blocks");
        assertThat(entry.recipe().normalizedIngredients().get(1).quantityAsDouble()).isEqualTo(2.0d);
    }
}
