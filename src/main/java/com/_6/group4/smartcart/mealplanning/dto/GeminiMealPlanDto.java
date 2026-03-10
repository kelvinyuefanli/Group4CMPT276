package com._6.group4.smartcart.mealplanning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiMealPlanDto(List<MealEntry> meals) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MealEntry(
        String dayOfWeek,
        String mealType,
        GeminiRecipeDto recipe
    ) {}
}
