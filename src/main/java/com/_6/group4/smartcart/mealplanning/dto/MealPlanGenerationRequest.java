package com._6.group4.smartcart.mealplanning.dto;

public record MealPlanGenerationRequest(
    String pantryIngredients,
    Integer servingSize,
    String dietaryRestrictions,
    String allergies,
    Boolean rotateCuisines,
    String preferredCuisines,
    String dislikedFoods,
    String mealSchedule
) {}
