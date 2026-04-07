package com._6.group4.smartcart.grocery;

import com._6.group4.smartcart.grocery.NutritionDatabase.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NutritionDatabaseTest {

    // ---- Lookup ----
    @Test void lookup_exactMatch() {
        NutritionPer100g n = NutritionDatabase.lookup("chicken breast");
        assertNotNull(n);
        assertEquals(165, n.calories());
        assertEquals(31.0, n.proteinG());
    }

    @Test void lookup_caseInsensitive() {
        assertNotNull(NutritionDatabase.lookup("SALMON"));
    }

    @Test void lookup_fuzzyMatch() {
        // "boneless skinless chicken thighs" contains "chicken thigh"
        NutritionPer100g n = NutritionDatabase.lookup("boneless skinless chicken thighs");
        assertNotNull(n);
        assertEquals(209, n.calories());
    }

    @Test void lookup_unknown() {
        assertNull(NutritionDatabase.lookup("dragon fruit"));
    }

    @Test void lookup_null() {
        assertNull(NutritionDatabase.lookup(null));
    }

    // ---- Estimate single ingredient ----
    @Test void estimate_gramsUnit() {
        IngredientNutrition n = NutritionDatabase.estimate("ground beef", 200.0, "g");
        assertNotNull(n);
        assertEquals(508, n.calories()); // 254 * 2
        assertEquals(34.4, n.proteinG());
    }

    @Test void estimate_cupsSpinach() {
        // 1 cup spinach ≈ 30g, spinach is 23 cal/100g → ~7 cal/cup
        IngredientNutrition n = NutritionDatabase.estimate("spinach", 2.0, "cup");
        assertNotNull(n);
        assertTrue(n.calories() > 10 && n.calories() < 20);
    }

    @Test void estimate_countEggs() {
        // 2 eggs, each ~50g, eggs are 155 cal/100g → ~155 cal for 2 eggs
        IngredientNutrition n = NutritionDatabase.estimate("eggs", 2.0, "count");
        assertNotNull(n);
        assertTrue(n.calories() > 140 && n.calories() < 170);
    }

    @Test void estimate_tbsp() {
        // 1 tbsp olive oil ≈ 15g, olive oil is 884 cal/100g → ~133 cal
        IngredientNutrition n = NutritionDatabase.estimate("olive oil", 1.0, "tbsp");
        assertNotNull(n);
        assertTrue(n.calories() > 120 && n.calories() < 145);
    }

    @Test void estimate_unknownIngredient() {
        assertNull(NutritionDatabase.estimate("zzz unknown item", 100.0, "g"));
    }

    @Test void estimate_nullQuantity() {
        assertNull(NutritionDatabase.estimate("chicken breast", null, "g"));
    }

    // ---- Recipe nutrition ----
    @Test void estimateRecipe_sumCorrectly() {
        List<RecipeIngredientInput> ingredients = List.of(
                new RecipeIngredientInput("chicken thighs", 400.0, "g"),
                new RecipeIngredientInput("broccoli", 2.0, "cup"),
                new RecipeIngredientInput("brown rice", 1.0, "cup")
        );
        RecipeNutrition rn = NutritionDatabase.estimateRecipe(ingredients);
        assertNotNull(rn);
        assertTrue(rn.totalCalories() > 900); // 836 + 62 + 228 ≈ 1126
        assertTrue(rn.totalProteinG() > 100);
        assertEquals(3, rn.items().size());
    }

    @Test void estimateRecipe_skipsUnknown() {
        List<RecipeIngredientInput> ingredients = List.of(
                new RecipeIngredientInput("chicken breast", 200.0, "g"),
                new RecipeIngredientInput("magic dust", 100.0, "g")
        );
        RecipeNutrition rn = NutritionDatabase.estimateRecipe(ingredients);
        assertEquals(1, rn.items().size()); // magic dust skipped
        assertEquals(330, rn.totalCalories()); // 165 * 2
    }

    @Test void estimateRecipe_emptyList() {
        RecipeNutrition rn = NutritionDatabase.estimateRecipe(List.of());
        assertEquals(0, rn.totalCalories());
        assertTrue(rn.items().isEmpty());
    }

    // ---- toMap ----
    @Test void nutritionPer100g_toMap() {
        NutritionPer100g n = new NutritionPer100g(200, 25.0, 10.0, 8.0, 3.0);
        var m = n.toMap();
        assertEquals(200, m.get("calories"));
        assertEquals(25.0, m.get("proteinG"));
    }

    @Test void recipeNutrition_toMap() {
        RecipeNutrition rn = new RecipeNutrition(500, 40.0, 60.0, 15.0, 5.0, List.of());
        var m = rn.toMap();
        assertEquals(500, m.get("totalCalories"));
        assertEquals(40.0, m.get("totalProteinG"));
    }
}
