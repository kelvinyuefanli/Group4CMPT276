package com._6.group4.smartcart.grocery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static nutrition lookup table for common grocery ingredients.
 * Values are per 100g (raw) from USDA FoodData Central.
 * No external API calls needed.
 */
public final class NutritionDatabase {

    private NutritionDatabase() {}

    public record NutritionPer100g(int calories, double proteinG, double carbsG, double fatG, double fiberG) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("calories", calories);
            m.put("proteinG", proteinG);
            m.put("carbsG", carbsG);
            m.put("fatG", fatG);
            m.put("fiberG", fiberG);
            return m;
        }
    }

    public record RecipeNutrition(int totalCalories, double totalProteinG, double totalCarbsG,
                                   double totalFatG, double totalFiberG, List<IngredientNutrition> items) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("totalCalories", totalCalories);
            m.put("totalProteinG", round1(totalProteinG));
            m.put("totalCarbsG", round1(totalCarbsG));
            m.put("totalFatG", round1(totalFatG));
            m.put("totalFiberG", round1(totalFiberG));
            return m;
        }
        private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    }

    public record IngredientNutrition(String name, int calories, double proteinG, double carbsG, double fatG) {}

    // ---- USDA-sourced nutrition per 100g (raw) ----
    private static final Map<String, NutritionPer100g> DB = new LinkedHashMap<>();
    static {
        // Poultry
        put("chicken breast", 165, 31.0, 0, 3.6, 0);
        put("chicken thigh", 209, 26.0, 0, 10.9, 0);
        put("chicken thighs", 209, 26.0, 0, 10.9, 0);
        put("ground chicken", 143, 17.4, 0, 8.1, 0);
        put("turkey breast", 135, 30.0, 0, 0.7, 0);
        put("ground turkey", 170, 21.0, 0, 9.4, 0);

        // Beef & Pork
        put("ground beef", 254, 17.2, 0, 20.0, 0);
        put("beef steak", 271, 26.1, 0, 18.0, 0);
        put("beef stew meat", 250, 26.0, 0, 15.4, 0);
        put("pork chops", 231, 25.7, 0, 13.5, 0);
        put("ground pork", 263, 16.9, 0, 21.2, 0);
        put("bacon", 541, 37.0, 1.4, 42.0, 0);
        put("sausage", 301, 12.0, 2.0, 27.0, 0);

        // Seafood
        put("salmon", 208, 20.4, 0, 13.4, 0);
        put("salmon fillet", 208, 20.4, 0, 13.4, 0);
        put("shrimp", 99, 24.0, 0.2, 0.3, 0);
        put("tilapia", 96, 20.1, 0, 1.7, 0);
        put("tuna", 132, 28.2, 0, 1.3, 0);
        put("cod", 82, 17.8, 0, 0.7, 0);

        // Eggs & Dairy
        put("egg", 155, 12.6, 1.1, 10.6, 0);
        put("eggs", 155, 12.6, 1.1, 10.6, 0);
        put("large eggs", 155, 12.6, 1.1, 10.6, 0);
        put("milk", 42, 3.4, 5.0, 1.0, 0);
        put("yogurt", 59, 10.0, 3.6, 0.4, 0);
        put("plain yogurt", 59, 10.0, 3.6, 0.4, 0);
        put("greek yogurt", 59, 10.0, 3.6, 0.4, 0);
        put("butter", 717, 0.9, 0.1, 81.1, 0);
        put("cheese", 402, 25.0, 1.3, 33.1, 0);
        put("cream cheese", 342, 5.9, 4.1, 34.2, 0);

        // Grains & Pasta
        put("rice", 130, 2.7, 28.2, 0.3, 0.4);
        put("brown rice", 123, 2.7, 25.6, 1.0, 1.8);
        put("white rice", 130, 2.7, 28.2, 0.3, 0.4);
        put("pasta", 131, 5.0, 25.0, 1.1, 1.8);
        put("whole wheat pasta", 124, 5.3, 26.5, 0.5, 4.5);
        put("quinoa", 120, 4.4, 21.3, 1.9, 2.8);
        put("oats", 389, 16.9, 66.3, 6.9, 10.6);
        put("rolled oats", 389, 16.9, 66.3, 6.9, 10.6);
        put("bread", 265, 9.4, 49.0, 3.2, 2.7);
        put("whole wheat bread", 247, 13.0, 41.3, 3.4, 6.8);
        put("tortilla", 312, 8.5, 52.0, 8.1, 3.5);
        put("whole wheat tortilla", 290, 9.0, 49.0, 7.0, 5.0);
        put("couscous", 112, 3.8, 23.2, 0.2, 1.4);
        put("pita bread", 275, 9.1, 55.7, 1.2, 2.2);
        put("whole wheat pita", 266, 9.8, 55.0, 1.7, 7.4);
        put("pancake mix", 350, 8.0, 72.0, 2.0, 2.0);

        // Vegetables
        put("broccoli", 34, 2.8, 7.0, 0.4, 2.6);
        put("spinach", 23, 2.9, 3.6, 0.4, 2.2);
        put("kale", 49, 4.3, 8.8, 0.9, 3.6);
        put("romaine lettuce", 17, 1.2, 3.3, 0.3, 2.1);
        put("mixed greens", 20, 1.5, 3.5, 0.3, 2.0);
        put("cabbage", 25, 1.3, 5.8, 0.1, 2.5);
        put("cauliflower", 25, 1.9, 5.0, 0.3, 2.0);
        put("brussels sprouts", 43, 3.4, 9.0, 0.3, 3.8);
        put("bell pepper", 31, 1.0, 6.0, 0.3, 2.1);
        put("bell peppers", 31, 1.0, 6.0, 0.3, 2.1);
        put("zucchini", 17, 1.2, 3.1, 0.3, 1.0);
        put("carrot", 41, 0.9, 9.6, 0.2, 2.8);
        put("carrots", 41, 0.9, 9.6, 0.2, 2.8);
        put("tomato", 18, 0.9, 3.9, 0.2, 1.2);
        put("tomatoes", 18, 0.9, 3.9, 0.2, 1.2);
        put("cucumber", 15, 0.7, 3.6, 0.1, 0.5);
        put("green beans", 31, 1.8, 7.0, 0.2, 3.4);
        put("corn", 86, 3.3, 19.0, 1.4, 2.7);
        put("peas", 81, 5.4, 14.5, 0.4, 5.7);
        put("sweet potato", 86, 1.6, 20.1, 0.1, 3.0);
        put("sweet potatoes", 86, 1.6, 20.1, 0.1, 3.0);
        put("potato", 77, 2.0, 17.5, 0.1, 2.2);
        put("potatoes", 77, 2.0, 17.5, 0.1, 2.2);
        put("butternut squash", 45, 1.0, 12.0, 0.1, 2.0);
        put("beets", 43, 1.6, 9.6, 0.2, 2.8);
        put("mushroom", 22, 3.1, 3.3, 0.3, 1.0);
        put("mushrooms", 22, 3.1, 3.3, 0.3, 1.0);
        put("asparagus", 20, 2.2, 3.9, 0.1, 2.1);
        put("celery", 16, 0.7, 3.0, 0.2, 1.6);
        put("eggplant", 25, 1.0, 6.0, 0.2, 3.0);
        put("onion", 40, 1.1, 9.3, 0.1, 1.7);
        put("onions", 40, 1.1, 9.3, 0.1, 1.7);
        put("garlic", 149, 6.4, 33.1, 0.5, 2.1);

        // Fruits
        put("banana", 89, 1.1, 22.8, 0.3, 2.6);
        put("bananas", 89, 1.1, 22.8, 0.3, 2.6);
        put("apple", 52, 0.3, 13.8, 0.2, 2.4);
        put("apples", 52, 0.3, 13.8, 0.2, 2.4);
        put("berries", 57, 0.7, 14.5, 0.3, 2.4);
        put("mixed berries", 57, 0.7, 14.5, 0.3, 2.4);
        put("strawberries", 32, 0.7, 7.7, 0.3, 2.0);
        put("blueberries", 57, 0.7, 14.5, 0.3, 2.4);
        put("orange", 47, 0.9, 11.8, 0.1, 2.4);
        put("oranges", 47, 0.9, 11.8, 0.1, 2.4);
        put("avocado", 160, 2.0, 8.5, 14.7, 6.7);
        put("mango", 60, 0.8, 15.0, 0.4, 1.6);
        put("lemon", 29, 1.1, 9.3, 0.3, 2.8);
        put("lemons", 29, 1.1, 9.3, 0.3, 2.8);
        put("lime", 30, 0.7, 10.5, 0.2, 2.8);
        put("limes", 30, 0.7, 10.5, 0.2, 2.8);

        // Legumes
        put("black beans", 132, 8.9, 23.7, 0.5, 8.7);
        put("chickpeas", 164, 8.9, 27.4, 2.6, 7.6);
        put("lentils", 116, 9.0, 20.1, 0.4, 7.9);
        put("tofu", 76, 8.1, 1.9, 4.8, 0.3);
        put("tempeh", 192, 20.3, 7.6, 10.8, 0);

        // Oils & Fats
        put("olive oil", 884, 0, 0, 100, 0);
        put("vegetable oil", 884, 0, 0, 100, 0);

        // Nuts & Seeds
        put("peanut butter", 588, 25.1, 20.0, 50.4, 6.0);
        put("almonds", 579, 21.2, 21.6, 49.9, 12.5);
        put("walnuts", 654, 15.2, 13.7, 65.2, 6.7);

        // Condiments (per 100g, but used in small amounts)
        put("soy sauce", 53, 8.1, 4.9, 0.6, 0.8);
        put("honey", 304, 0.3, 82.4, 0, 0.2);
        put("ketchup", 112, 1.7, 25.8, 0.1, 0.3);
        put("mustard", 66, 4.4, 5.3, 3.3, 3.3);
        put("mayonnaise", 680, 1.0, 0.6, 75.0, 0);
        put("vinegar", 18, 0, 0.04, 0, 0);
        put("hot sauce", 11, 0.5, 1.8, 0.4, 0.7);
    }

    private static void put(String name, int cal, double prot, double carb, double fat, double fiber) {
        DB.put(name.toLowerCase(Locale.ROOT), new NutritionPer100g(cal, prot, carb, fat, fiber));
    }

    /**
     * Look up nutrition for a single ingredient by canonical name.
     * Returns null if not found.
     */
    public static NutritionPer100g lookup(String ingredientName) {
        if (ingredientName == null) return null;
        String key = ingredientName.toLowerCase(Locale.ROOT).trim();
        NutritionPer100g exact = DB.get(key);
        if (exact != null) return exact;

        // Fuzzy match: check if any DB key is contained in the ingredient name
        for (Map.Entry<String, NutritionPer100g> entry : DB.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Estimate nutrition for a specific quantity of an ingredient.
     * Converts common units to grams, then scales per-100g values.
     */
    public static IngredientNutrition estimate(String ingredientName, Double quantity, String unit) {
        NutritionPer100g base = lookup(ingredientName);
        if (base == null || quantity == null) return null;

        double grams = toGrams(ingredientName, quantity, unit);
        double scale = grams / 100.0;

        return new IngredientNutrition(
                ingredientName,
                (int) Math.round(base.calories() * scale),
                Math.round(base.proteinG() * scale * 10.0) / 10.0,
                Math.round(base.carbsG() * scale * 10.0) / 10.0,
                Math.round(base.fatG() * scale * 10.0) / 10.0
        );
    }

    /**
     * Estimate total nutrition for a list of ingredient lines.
     */
    public static RecipeNutrition estimateRecipe(List<RecipeIngredientInput> ingredients) {
        int totalCal = 0;
        double totalProt = 0, totalCarb = 0, totalFat = 0, totalFiber = 0;
        List<IngredientNutrition> items = new java.util.ArrayList<>();

        for (RecipeIngredientInput ing : ingredients) {
            IngredientNutrition est = estimate(ing.name(), ing.quantity(), ing.unit());
            if (est != null) {
                totalCal += est.calories();
                totalProt += est.proteinG();
                totalCarb += est.carbsG();
                totalFat += est.fatG();
                items.add(est);
            }
        }

        return new RecipeNutrition(totalCal, totalProt, totalCarb, totalFat, totalFiber, items);
    }

    public record RecipeIngredientInput(String name, Double quantity, String unit) {}

    // ---- Unit to grams conversion ----
    private static double toGrams(String name, double quantity, String unit) {
        if (unit == null || unit.isBlank()) return quantity; // assume grams if no unit
        String u = unit.toLowerCase(Locale.ROOT).trim();

        // Direct weight units
        if (u.equals("g") || u.equals("gram") || u.equals("grams")) return quantity;
        if (u.equals("kg") || u.equals("kilogram")) return quantity * 1000;
        if (u.equals("oz") || u.equals("ounce") || u.equals("ounces")) return quantity * 28.35;
        if (u.equals("lb") || u.equals("lbs") || u.equals("pound")) return quantity * 453.6;

        // Volume (approximate weights by ingredient type)
        if (u.equals("cup") || u.equals("cups")) return cupsToGrams(name, quantity);
        if (u.equals("tbsp") || u.equals("tablespoon") || u.equals("tablespoons")) return quantity * 15;
        if (u.equals("tsp") || u.equals("teaspoon") || u.equals("teaspoons")) return quantity * 5;

        // Count-based (approximate weight per piece)
        if (u.equals("count") || u.equals("piece") || u.equals("pieces")
                || u.equals("large") || u.equals("medium") || u.equals("small")
                || u.equals("whole") || u.equals("slice") || u.equals("slices")) {
            return countToGrams(name, quantity);
        }

        // Default: assume grams
        return quantity;
    }

    private static double cupsToGrams(String name, double cups) {
        String n = name.toLowerCase(Locale.ROOT);
        // Leafy greens: 1 cup ≈ 30g
        if (n.contains("spinach") || n.contains("kale") || n.contains("lettuce") || n.contains("greens"))
            return cups * 30;
        // Broccoli/cauliflower: 1 cup ≈ 91g
        if (n.contains("broccoli") || n.contains("cauliflower")) return cups * 91;
        // Rice/grains cooked: 1 cup ≈ 185g
        if (n.contains("rice") || n.contains("quinoa") || n.contains("couscous")) return cups * 185;
        // Pasta cooked: 1 cup ≈ 140g
        if (n.contains("pasta")) return cups * 140;
        // Berries: 1 cup ≈ 150g
        if (n.contains("berr")) return cups * 150;
        // Yogurt: 1 cup ≈ 245g
        if (n.contains("yogurt")) return cups * 245;
        // Milk: 1 cup ≈ 244g
        if (n.contains("milk")) return cups * 244;
        // Oats: 1 cup ≈ 81g
        if (n.contains("oat")) return cups * 81;
        // Flour: 1 cup ≈ 125g
        if (n.contains("flour")) return cups * 125;
        // Chopped vegetables: 1 cup ≈ 130g
        return cups * 130;
    }

    private static double countToGrams(String name, double count) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("egg")) return count * 50;          // 1 large egg ≈ 50g
        if (n.contains("banana")) return count * 118;      // 1 medium banana
        if (n.contains("apple")) return count * 182;       // 1 medium apple
        if (n.contains("orange")) return count * 131;      // 1 medium orange
        if (n.contains("bell pepper")) return count * 119; // 1 medium pepper
        if (n.contains("carrot")) return count * 61;       // 1 medium carrot
        if (n.contains("potato")) return count * 150;      // 1 medium potato
        if (n.contains("sweet potato")) return count * 130;
        if (n.contains("onion")) return count * 110;
        if (n.contains("tomato")) return count * 123;
        if (n.contains("tortilla")) return count * 49;
        if (n.contains("bread") || n.contains("slice")) return count * 30;
        if (n.contains("pita")) return count * 60;
        if (n.contains("chicken")) return count * 130;     // 1 thigh/breast
        if (n.contains("salmon") || n.contains("fillet")) return count * 170; // 1 fillet
        // Default: 100g per piece
        return count * 100;
    }
}
