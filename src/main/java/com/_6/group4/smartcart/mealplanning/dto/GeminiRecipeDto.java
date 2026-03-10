package com._6.group4.smartcart.mealplanning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiRecipeDto(
    String title,
    String cuisine,
    Integer cookTimeMinutes,
    Integer servings,
    String instructions,
    List<IngredientDto> ingredients
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IngredientDto(String name, Double quantity, String unit) {}
}
