package com._6.group4.smartcart.mealplanning.dto;

import java.util.List;

public record GeminiRecipeDto(
    String title,
    String cuisine,
    Integer cookTimeMinutes,
    Integer servings,
    String instructions,
    List<IngredientDto> ingredients
) {
    public record IngredientDto(String name, Double quantity, String unit) {}
}
