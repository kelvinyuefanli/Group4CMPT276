package com._6.group4.smartcart.mealplanning.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiMealPlanDto(List<MealEntry> meals) {
    public GeminiMealPlanDto {
        meals = meals == null ? List.of() : meals;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static GeminiMealPlanDto from(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        JsonNode mealsNode = GeminiRecipeDto.firstPresent(node, "meals", "mealPlan", "entries");
        if (mealsNode == null || mealsNode.isNull()) {
            return new GeminiMealPlanDto(List.of());
        }

        List<MealEntry> parsedMeals = new ArrayList<>();
        if (mealsNode.isArray()) {
            for (JsonNode mealNode : mealsNode) {
                MealEntry entry = MealEntry.from(mealNode);
                if (entry != null) {
                    parsedMeals.add(entry);
                }
            }
            return new GeminiMealPlanDto(parsedMeals);
        }

        MealEntry singleEntry = MealEntry.from(mealsNode);
        return singleEntry == null ? new GeminiMealPlanDto(List.of()) : new GeminiMealPlanDto(List.of(singleEntry));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MealEntry(
        String dayOfWeek,
        String mealType,
        GeminiRecipeDto recipe
    ) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static MealEntry from(JsonNode node) {
            if (node == null || node.isNull()) {
                return null;
            }

            return new MealEntry(
                GeminiRecipeDto.readInlineText(GeminiRecipeDto.firstPresent(node, "dayOfWeek", "day")),
                GeminiRecipeDto.readInlineText(GeminiRecipeDto.firstPresent(node, "mealType", "meal", "type")),
                GeminiRecipeDto.from(GeminiRecipeDto.firstPresent(node, "recipe"))
            );
        }
    }
}
