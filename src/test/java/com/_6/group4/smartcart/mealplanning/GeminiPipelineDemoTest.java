package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.mealplanning.dto.GeminiMealPlanDto;
import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that feeds 5 realistic demo Gemini JSON responses through
 * the full parsing + DB persistence pipeline and dumps the resulting database
 * rows.  No real API key needed -- the JSON is hardcoded.
 */
@SpringBootTest
@ActiveProfiles("dev")
class GeminiPipelineDemoTest {

    @Autowired
    private RecipeRepository recipeRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    // ------------------------------------------------------------------ //
    //  DEMO 1 -- Single recipe: Italian Chicken Pasta
    // ------------------------------------------------------------------ //
    private static final String DEMO_1_RECIPE = """
        {
          "title": "Creamy Garlic Chicken Pasta",
          "cuisine": "Italian",
          "cookTimeMinutes": 35,
          "servings": 4,
          "instructions": "1. Cook penne pasta according to package directions. 2. Season chicken breasts with salt, pepper, and Italian seasoning. 3. Heat olive oil in a large skillet over medium-high heat. Sear chicken 6 min per side until golden. Remove and slice. 4. In the same skillet, sauté minced garlic for 30 seconds. Add heavy cream and parmesan. Stir until sauce thickens (3-4 min). 5. Toss pasta and chicken into the sauce. Garnish with fresh basil.",
          "ingredients": [
            { "name": "penne pasta", "quantity": 400.0, "unit": "g" },
            { "name": "chicken breast", "quantity": 2.0, "unit": "pieces" },
            { "name": "garlic cloves", "quantity": 4.0, "unit": "cloves" },
            { "name": "heavy cream", "quantity": 1.0, "unit": "cup" },
            { "name": "parmesan cheese", "quantity": 0.5, "unit": "cup" },
            { "name": "olive oil", "quantity": 2.0, "unit": "tbsp" },
            { "name": "fresh basil", "quantity": null, "unit": "handful" }
          ]
        }""";

    // ------------------------------------------------------------------ //
    //  DEMO 2 -- Single recipe: Japanese Teriyaki Salmon
    // ------------------------------------------------------------------ //
    private static final String DEMO_2_RECIPE = """
        {
          "title": "Teriyaki Glazed Salmon",
          "cuisine": "Japanese",
          "cookTimeMinutes": 20,
          "servings": 2,
          "instructions": "1. Whisk together soy sauce, mirin, brown sugar, and grated ginger in a small bowl. 2. Place salmon fillets skin-side down in an oven-safe skillet. 3. Pour teriyaki sauce over salmon. 4. Bake at 400°F (200°C) for 12-15 minutes until fish flakes easily. 5. Broil for 2 minutes to caramelize the glaze. 6. Serve over steamed rice, garnished with sesame seeds and sliced green onion.",
          "ingredients": [
            { "name": "salmon fillets", "quantity": 2.0, "unit": "pieces" },
            { "name": "soy sauce", "quantity": 3.0, "unit": "tbsp" },
            { "name": "mirin", "quantity": 2.0, "unit": "tbsp" },
            { "name": "brown sugar", "quantity": 1.0, "unit": "tbsp" },
            { "name": "fresh ginger", "quantity": 1.0, "unit": "tsp" },
            { "name": "steamed rice", "quantity": 2.0, "unit": "cups" },
            { "name": "sesame seeds", "quantity": 1.0, "unit": "tsp" },
            { "name": "green onion", "quantity": 2.0, "unit": "stalks" }
          ]
        }""";

    // ------------------------------------------------------------------ //
    //  DEMO 3 -- Single recipe: Mexican Tacos (with markdown fences)
    // ------------------------------------------------------------------ //
    private static final String DEMO_3_RECIPE_WITH_FENCES = """
        ```json
        {
          "title": "Spicy Beef Tacos",
          "cuisine": "Mexican",
          "cookTimeMinutes": 25,
          "servings": 6,
          "instructions": "1. Brown ground beef in a skillet over medium heat, breaking into crumbles. 2. Add taco seasoning, cumin, chili powder, and a splash of water. Simmer 5 min. 3. Warm corn tortillas in a dry pan or microwave. 4. Assemble tacos with beef, diced onion, chopped cilantro, salsa, and a squeeze of lime. 5. Top with crumbled queso fresco.",
          "ingredients": [
            { "name": "ground beef", "quantity": 500.0, "unit": "g" },
            { "name": "taco seasoning", "quantity": 2.0, "unit": "tbsp" },
            { "name": "cumin", "quantity": 1.0, "unit": "tsp" },
            { "name": "chili powder", "quantity": 1.0, "unit": "tsp" },
            { "name": "corn tortillas", "quantity": 12.0, "unit": "pieces" },
            { "name": "white onion", "quantity": 1.0, "unit": "medium" },
            { "name": "fresh cilantro", "quantity": 0.25, "unit": "cup" },
            { "name": "lime", "quantity": 2.0, "unit": "pieces" },
            { "name": "queso fresco", "quantity": 0.5, "unit": "cup" },
            { "name": "salsa", "quantity": 0.5, "unit": "cup" }
          ]
        }
        ```""";

    // ------------------------------------------------------------------ //
    //  DEMO 4 -- Single recipe: Vegan Buddha Bowl (no cuisine)
    // ------------------------------------------------------------------ //
    private static final String DEMO_4_RECIPE_NO_CUISINE = """
        {
          "title": "Rainbow Vegan Buddha Bowl",
          "cuisine": null,
          "cookTimeMinutes": 30,
          "servings": 2,
          "instructions": "1. Cook quinoa according to package directions. 2. Roast chickpeas with olive oil, paprika, and garlic powder at 425°F for 20 minutes. 3. Slice avocado, shred purple cabbage, and julienne carrots. 4. Assemble bowls with quinoa base, roasted chickpeas, and all vegetables. 5. Drizzle with tahini dressing (tahini, lemon juice, water, pinch of salt).",
          "ingredients": [
            { "name": "quinoa", "quantity": 1.0, "unit": "cup" },
            { "name": "canned chickpeas", "quantity": 1.0, "unit": "can" },
            { "name": "avocado", "quantity": 1.0, "unit": "whole" },
            { "name": "purple cabbage", "quantity": 1.0, "unit": "cup" },
            { "name": "carrots", "quantity": 2.0, "unit": "medium" },
            { "name": "tahini", "quantity": 2.0, "unit": "tbsp" },
            { "name": "lemon juice", "quantity": 1.0, "unit": "tbsp" },
            { "name": "olive oil", "quantity": 1.0, "unit": "tbsp" },
            { "name": "paprika", "quantity": 1.0, "unit": "tsp" }
          ]
        }""";

    // ------------------------------------------------------------------ //
    //  DEMO 5 -- Meal plan: 3 meals for Monday (subset to keep test readable)
    // ------------------------------------------------------------------ //
    private static final String DEMO_5_MEAL_PLAN = """
        {
          "meals": [
            {
              "dayOfWeek": "MONDAY",
              "mealType": "BREAKFAST",
              "recipe": {
                "title": "Veggie Omelette",
                "cuisine": "American",
                "cookTimeMinutes": 10,
                "servings": 1,
                "instructions": "1. Whisk 3 eggs with salt and pepper. 2. Heat butter in a non-stick pan. 3. Pour eggs, cook 1 min. 4. Add diced bell pepper, spinach, and feta. 5. Fold and cook 2 more minutes.",
                "ingredients": [
                  { "name": "eggs", "quantity": 3.0, "unit": "pieces" },
                  { "name": "bell pepper", "quantity": 0.5, "unit": "whole" },
                  { "name": "spinach", "quantity": 0.5, "unit": "cup" },
                  { "name": "feta cheese", "quantity": 2.0, "unit": "tbsp" },
                  { "name": "butter", "quantity": 1.0, "unit": "tbsp" }
                ]
              }
            },
            {
              "dayOfWeek": "MONDAY",
              "mealType": "LUNCH",
              "recipe": {
                "title": "Chicken Caesar Wrap",
                "cuisine": "American",
                "cookTimeMinutes": 15,
                "servings": 1,
                "instructions": "1. Grill seasoned chicken breast, slice thinly. 2. Toss romaine lettuce with Caesar dressing and croutons. 3. Place on a large flour tortilla, add chicken. 4. Roll tightly and slice in half.",
                "ingredients": [
                  { "name": "chicken breast", "quantity": 1.0, "unit": "piece" },
                  { "name": "romaine lettuce", "quantity": 2.0, "unit": "cups" },
                  { "name": "caesar dressing", "quantity": 2.0, "unit": "tbsp" },
                  { "name": "flour tortilla", "quantity": 1.0, "unit": "large" },
                  { "name": "croutons", "quantity": 0.25, "unit": "cup" }
                ]
              }
            },
            {
              "dayOfWeek": "MONDAY",
              "mealType": "DINNER",
              "recipe": {
                "title": "Beef Stir-Fry with Vegetables",
                "cuisine": "Chinese",
                "cookTimeMinutes": 20,
                "servings": 2,
                "instructions": "1. Slice beef sirloin into thin strips. 2. Marinate in soy sauce, sesame oil, and cornstarch for 10 min. 3. Stir-fry beef in a hot wok with vegetable oil for 2 minutes. Remove. 4. Stir-fry broccoli, snap peas, and bell pepper for 3 minutes. 5. Return beef to wok, add oyster sauce, toss and serve over rice.",
                "ingredients": [
                  { "name": "beef sirloin", "quantity": 300.0, "unit": "g" },
                  { "name": "soy sauce", "quantity": 2.0, "unit": "tbsp" },
                  { "name": "sesame oil", "quantity": 1.0, "unit": "tsp" },
                  { "name": "broccoli", "quantity": 1.0, "unit": "cup" },
                  { "name": "snap peas", "quantity": 0.5, "unit": "cup" },
                  { "name": "bell pepper", "quantity": 1.0, "unit": "whole" },
                  { "name": "oyster sauce", "quantity": 2.0, "unit": "tbsp" },
                  { "name": "rice", "quantity": 2.0, "unit": "cups" }
                ]
              }
            }
          ]
        }""";

    // ======================== HELPERS ================================= //

    /** Same logic as GeminiService.parseJson -- strips markdown fences then parses. */
    private <T> T parseJson(String raw, Class<T> type) throws Exception {
        String cleaned = raw.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "");
            cleaned = cleaned.replaceFirst("```\\s*$", "");
            cleaned = cleaned.strip();
        }
        return mapper.readValue(cleaned, type);
    }

    /** Same logic as MealPlanController.persistRecipe */
    private Recipe persistRecipe(GeminiRecipeDto dto) {
        Recipe recipe = new Recipe(dto.title() != null ? dto.title() : "Untitled Recipe");
        recipe.setInstructions(dto.instructions());
        recipe.setCookTimeMinutes(dto.cookTimeMinutes());
        recipe.setServings(dto.servings());
        recipe.setCuisine(dto.cuisine());
        recipe.setSource("gemini");

        if (dto.ingredients() != null) {
            for (Object ing : dto.ingredients()) {
                if (ing instanceof java.util.Map<?, ?> ingMap) {
                    String name = ingMap.get("name") != null ? ingMap.get("name").toString() : "unknown";
                    RecipeIngredient ri = new RecipeIngredient(recipe, name.trim());
                    Object qtyObj = ingMap.get("quantity");
                    if (qtyObj instanceof Number n) ri.setQuantity(n.doubleValue());
                    Object unitObj = ingMap.get("unit");
                    if (unitObj != null) ri.setUnit(unitObj.toString().trim());
                    recipe.getIngredients().add(ri);
                } else if (ing instanceof String s) {
                    recipe.getIngredients().add(new RecipeIngredient(recipe, s.trim()));
                }
            }
        }
        return recipeRepository.save(recipe);
    }

    private void dumpRecipe(Recipe r) {
        System.out.println("  ┌─ Recipe #" + r.getId());
        System.out.println("  │  title:    " + r.getTitle());
        System.out.println("  │  cuisine:  " + r.getCuisine());
        System.out.println("  │  cook:     " + r.getCookTimeMinutes() + " min");
        System.out.println("  │  servings: " + r.getServings());
        System.out.println("  │  source:   " + r.getSource());
        System.out.println("  │  created:  " + r.getCreatedAt());
        System.out.println("  │  instructions: " + truncate(r.getInstructions(), 100));
        System.out.println("  │  ingredients (" + r.getIngredients().size() + "):");
        for (RecipeIngredient ri : r.getIngredients()) {
            System.out.printf("  │    - %-25s %s %s%n",
                ri.getIngredientName(),
                ri.getQuantity() != null ? ri.getQuantity() : "—",
                ri.getUnit() != null ? ri.getUnit() : "");
        }
        System.out.println("  └──────────────────────────────────");
    }

    private String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    // ======================== THE TEST ================================ //

    @Test
    @Transactional
    void demoFiveGeminiResponses() throws Exception {

        // -------- DEMO 1: Italian Chicken Pasta --------
        System.out.println("\n========== DEMO 1: Single Recipe — Italian Chicken Pasta ==========");
        GeminiRecipeDto dto1 = parseJson(DEMO_1_RECIPE, GeminiRecipeDto.class);
        System.out.println("  Parsed DTO title: " + dto1.title());
        System.out.println("  Parsed DTO ingredients count: " + dto1.ingredients().size());
        Recipe saved1 = persistRecipe(dto1);
        System.out.println("  >> Saved to DB:");
        dumpRecipe(saved1);

        // -------- DEMO 2: Japanese Teriyaki Salmon --------
        System.out.println("\n========== DEMO 2: Single Recipe — Teriyaki Glazed Salmon ==========");
        GeminiRecipeDto dto2 = parseJson(DEMO_2_RECIPE, GeminiRecipeDto.class);
        System.out.println("  Parsed DTO title: " + dto2.title());
        System.out.println("  Parsed DTO ingredients count: " + dto2.ingredients().size());
        Recipe saved2 = persistRecipe(dto2);
        System.out.println("  >> Saved to DB:");
        dumpRecipe(saved2);

        // -------- DEMO 3: Mexican Tacos (with markdown fences) --------
        System.out.println("\n========== DEMO 3: Single Recipe — Spicy Beef Tacos (markdown fenced) ==========");
        GeminiRecipeDto dto3 = parseJson(DEMO_3_RECIPE_WITH_FENCES, GeminiRecipeDto.class);
        System.out.println("  Parsed DTO title: " + dto3.title());
        System.out.println("  Parsed DTO ingredients count: " + dto3.ingredients().size());
        Recipe saved3 = persistRecipe(dto3);
        System.out.println("  >> Saved to DB:");
        dumpRecipe(saved3);

        // -------- DEMO 4: Vegan Buddha Bowl (null cuisine) --------
        System.out.println("\n========== DEMO 4: Single Recipe — Vegan Buddha Bowl (null cuisine) ==========");
        GeminiRecipeDto dto4 = parseJson(DEMO_4_RECIPE_NO_CUISINE, GeminiRecipeDto.class);
        System.out.println("  Parsed DTO title: " + dto4.title());
        System.out.println("  Parsed DTO cuisine: " + dto4.cuisine());
        System.out.println("  Parsed DTO ingredients count: " + dto4.ingredients().size());
        Recipe saved4 = persistRecipe(dto4);
        System.out.println("  >> Saved to DB:");
        dumpRecipe(saved4);

        // -------- DEMO 5: Meal Plan (Monday 3 meals) --------
        System.out.println("\n========== DEMO 5: Meal Plan — Monday (BREAKFAST / LUNCH / DINNER) ==========");
        GeminiMealPlanDto plan = parseJson(DEMO_5_MEAL_PLAN, GeminiMealPlanDto.class);
        System.out.println("  Parsed meal entries: " + plan.meals().size());
        for (GeminiMealPlanDto.MealEntry entry : plan.meals()) {
            System.out.println("  [" + entry.dayOfWeek() + " / " + entry.mealType() + "] -> " + entry.recipe().title());
            Recipe savedMeal = persistRecipe(entry.recipe());
            dumpRecipe(savedMeal);
        }

        // -------- FULL DB DUMP --------
        System.out.println("\n========== FULL DATABASE STATE ==========");
        List<Recipe> all = recipeRepository.findAllByOrderByCreatedAtDesc();
        System.out.println("Total recipes in DB: " + all.size());
        int totalIngredients = 0;
        for (Recipe r : all) {
            totalIngredients += r.getIngredients().size();
            dumpRecipe(r);
        }
        System.out.println("Total ingredient rows in DB: " + totalIngredients);

        // -------- ASSERTIONS --------
        assertThat(all).hasSize(7); // 4 single + 3 from meal plan
        assertThat(totalIngredients).isEqualTo(7 + 8 + 10 + 9 + 5 + 5 + 8); // 52

        assertThat(all).extracting(Recipe::getTitle).contains(
            "Creamy Garlic Chicken Pasta",
            "Teriyaki Glazed Salmon",
            "Spicy Beef Tacos",
            "Rainbow Vegan Buddha Bowl",
            "Veggie Omelette",
            "Chicken Caesar Wrap",
            "Beef Stir-Fry with Vegetables"
        );
    }
}
