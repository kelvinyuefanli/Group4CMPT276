package com._6.group4.smartcart.mealplanning.dto;

import java.util.List;

public record GeminiMealPlanDto(List<MealEntry> meals) {
    public record MealEntry(
        String dayOfWeek,
        String mealType,
        GeminiRecipeDto recipe
    ) {}
}
