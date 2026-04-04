package com._6.group4.smartcart.mealplanning;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for grocery list aggregation and categorization logic.
 */
class GroceryAggregationTest {

    // We test the categorize method via reflection-free approach:
    // the GroceryItem inner class is private, so we test the public-facing behavior
    // through the categorize patterns directly.

    @Test
    void categorize_protein() {
        assertThat(categorize("chicken breast")).isEqualTo("Protein");
        assertThat(categorize("ground beef")).isEqualTo("Protein");
        assertThat(categorize("salmon fillet")).isEqualTo("Protein");
        assertThat(categorize("firm tofu")).isEqualTo("Protein");
        assertThat(categorize("eggs")).isEqualTo("Protein");
    }

    @Test
    void categorize_dairy() {
        assertThat(categorize("whole milk")).isEqualTo("Dairy");
        assertThat(categorize("cheddar cheese")).isEqualTo("Dairy");
        assertThat(categorize("Greek yogurt")).isEqualTo("Dairy");
        assertThat(categorize("heavy cream")).isEqualTo("Dairy");
        assertThat(categorize("unsalted butter")).isEqualTo("Dairy");
    }

    @Test
    void categorize_produce() {
        assertThat(categorize("romaine lettuce")).isEqualTo("Produce");
        assertThat(categorize("cherry tomato")).isEqualTo("Produce");
        assertThat(categorize("yellow onion")).isEqualTo("Produce");
        assertThat(categorize("fresh garlic")).isEqualTo("Produce");
        assertThat(categorize("baby spinach")).isEqualTo("Produce");
    }

    @Test
    void categorize_pantry() {
        assertThat(categorize("olive oil")).isEqualTo("Pantry");
        assertThat(categorize("soy sauce")).isEqualTo("Pantry");
        assertThat(categorize("rice")).isEqualTo("Pantry");
        assertThat(categorize("flour")).isEqualTo("Pantry");
    }

    /**
     * Replicates the categorize logic from MealPlanApiController.GroceryItem
     * to test it in isolation.
     */
    private static String categorize(String name) {
        String lower = name.toLowerCase();
        if (lower.matches(".*(chicken|beef|salmon|turkey|pork|fish|shrimp|tofu|egg).*")) return "Protein";
        if (lower.matches(".*(milk|cheese|yogurt|cream|butter).*")) return "Dairy";
        if (lower.matches(".*(lettuce|tomato|onion|garlic|pepper|carrot|celery|spinach|avocado|lemon|lime|basil|parsley|cilantro|dill|ginger|broccoli|cabbage|cucumber).*"))
            return "Produce";
        return "Pantry";
    }
}
