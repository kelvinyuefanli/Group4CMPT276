package com._6.group4.smartcart.grocery;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Suggests ingredient substitutions within the same food category.
 * E.g., swap salmon for chicken thighs, broccoli for asparagus.
 */
public final class IngredientSwapService {

    private IngredientSwapService() {}

    public enum Category {
        POULTRY, RED_MEAT, SEAFOOD, PLANT_PROTEIN,
        LEAFY_GREEN, CRUCIFEROUS, ROOT_VEGETABLE, EVERYDAY_VEGGIE,
        GRAIN, PASTA, BREAD,
        FRUIT, BERRY,
        DAIRY, EGG,
        OIL, CONDIMENT,
        UNKNOWN
    }

    public record SwapOption(String name, String displayName, double quantityRatio, String note) {}

    // ---- Category membership ----
    private static final Map<String, Category> INGREDIENT_CATEGORIES = new LinkedHashMap<>();
    private static final Map<Category, List<SwapOption>> SWAP_OPTIONS = new LinkedHashMap<>();

    static {
        // Poultry
        cat("chicken breast", Category.POULTRY);
        cat("chicken thigh", Category.POULTRY);
        cat("chicken thighs", Category.POULTRY);
        cat("ground chicken", Category.POULTRY);
        cat("turkey breast", Category.POULTRY);
        cat("ground turkey", Category.POULTRY);

        // Red meat
        cat("ground beef", Category.RED_MEAT);
        cat("beef steak", Category.RED_MEAT);
        cat("beef stew meat", Category.RED_MEAT);
        cat("pork chops", Category.RED_MEAT);
        cat("ground pork", Category.RED_MEAT);

        // Seafood
        cat("salmon", Category.SEAFOOD);
        cat("salmon fillet", Category.SEAFOOD);
        cat("shrimp", Category.SEAFOOD);
        cat("tilapia", Category.SEAFOOD);
        cat("tuna", Category.SEAFOOD);
        cat("cod", Category.SEAFOOD);

        // Plant protein
        cat("tofu", Category.PLANT_PROTEIN);
        cat("tempeh", Category.PLANT_PROTEIN);
        cat("black beans", Category.PLANT_PROTEIN);
        cat("chickpeas", Category.PLANT_PROTEIN);
        cat("lentils", Category.PLANT_PROTEIN);

        // Leafy greens
        cat("spinach", Category.LEAFY_GREEN);
        cat("kale", Category.LEAFY_GREEN);
        cat("romaine lettuce", Category.LEAFY_GREEN);
        cat("mixed greens", Category.LEAFY_GREEN);

        // Cruciferous
        cat("broccoli", Category.CRUCIFEROUS);
        cat("cauliflower", Category.CRUCIFEROUS);
        cat("brussels sprouts", Category.CRUCIFEROUS);
        cat("cabbage", Category.CRUCIFEROUS);

        // Root vegetables
        cat("carrot", Category.ROOT_VEGETABLE);
        cat("carrots", Category.ROOT_VEGETABLE);
        cat("sweet potato", Category.ROOT_VEGETABLE);
        cat("sweet potatoes", Category.ROOT_VEGETABLE);
        cat("potato", Category.ROOT_VEGETABLE);
        cat("potatoes", Category.ROOT_VEGETABLE);
        cat("beets", Category.ROOT_VEGETABLE);

        // Everyday veggies
        cat("bell pepper", Category.EVERYDAY_VEGGIE);
        cat("bell peppers", Category.EVERYDAY_VEGGIE);
        cat("zucchini", Category.EVERYDAY_VEGGIE);
        cat("tomato", Category.EVERYDAY_VEGGIE);
        cat("tomatoes", Category.EVERYDAY_VEGGIE);
        cat("cucumber", Category.EVERYDAY_VEGGIE);
        cat("green beans", Category.EVERYDAY_VEGGIE);
        cat("mushroom", Category.EVERYDAY_VEGGIE);
        cat("mushrooms", Category.EVERYDAY_VEGGIE);
        cat("asparagus", Category.EVERYDAY_VEGGIE);
        cat("celery", Category.EVERYDAY_VEGGIE);
        cat("corn", Category.EVERYDAY_VEGGIE);
        cat("eggplant", Category.EVERYDAY_VEGGIE);

        // Grains
        cat("rice", Category.GRAIN);
        cat("brown rice", Category.GRAIN);
        cat("white rice", Category.GRAIN);
        cat("quinoa", Category.GRAIN);
        cat("couscous", Category.GRAIN);

        // Pasta
        cat("pasta", Category.PASTA);
        cat("whole wheat pasta", Category.PASTA);

        // Bread
        cat("bread", Category.BREAD);
        cat("whole wheat bread", Category.BREAD);
        cat("tortilla", Category.BREAD);
        cat("whole wheat tortilla", Category.BREAD);
        cat("pita bread", Category.BREAD);

        // Fruit
        cat("banana", Category.FRUIT);
        cat("bananas", Category.FRUIT);
        cat("apple", Category.FRUIT);
        cat("apples", Category.FRUIT);
        cat("orange", Category.FRUIT);
        cat("oranges", Category.FRUIT);
        cat("mango", Category.FRUIT);
        cat("avocado", Category.FRUIT);

        // Berries
        cat("berries", Category.BERRY);
        cat("mixed berries", Category.BERRY);
        cat("strawberries", Category.BERRY);
        cat("blueberries", Category.BERRY);

        // Dairy
        cat("milk", Category.DAIRY);
        cat("yogurt", Category.DAIRY);
        cat("plain yogurt", Category.DAIRY);
        cat("greek yogurt", Category.DAIRY);
        cat("cheese", Category.DAIRY);

        // Eggs
        cat("egg", Category.EGG);
        cat("eggs", Category.EGG);
        cat("large eggs", Category.EGG);

        // Build swap options per category
        for (Category c : Category.values()) {
            SWAP_OPTIONS.put(c, new ArrayList<>());
        }

        // Cross-category swaps (protein ↔ protein)
        addCrossSwaps(Category.POULTRY, Category.RED_MEAT, 1.0, "Different protein type");
        addCrossSwaps(Category.POULTRY, Category.SEAFOOD, 0.85, "Seafood cooks faster");
        addCrossSwaps(Category.POULTRY, Category.PLANT_PROTEIN, 1.2, "Use more for same protein");
        addCrossSwaps(Category.RED_MEAT, Category.SEAFOOD, 0.85, "Lighter option");
        addCrossSwaps(Category.RED_MEAT, Category.PLANT_PROTEIN, 1.2, "Use more for same protein");
        addCrossSwaps(Category.SEAFOOD, Category.PLANT_PROTEIN, 1.2, "Use more for same protein");
    }

    private static void cat(String name, Category category) {
        INGREDIENT_CATEGORIES.put(name.toLowerCase(Locale.ROOT), category);
    }

    private static void addCrossSwaps(Category from, Category to, double ratio, String note) {
        // Items in 'to' category become swap options for items in 'from' category
        for (Map.Entry<String, Category> entry : INGREDIENT_CATEGORIES.entrySet()) {
            if (entry.getValue() == to) {
                SWAP_OPTIONS.computeIfAbsent(from, k -> new ArrayList<>())
                        .add(new SwapOption(entry.getKey(), capitalize(entry.getKey()), ratio, note));
            }
            if (entry.getValue() == from) {
                SWAP_OPTIONS.computeIfAbsent(to, k -> new ArrayList<>())
                        .add(new SwapOption(entry.getKey(), capitalize(entry.getKey()), 1.0 / Math.max(ratio, 0.01), note));
            }
        }
    }

    /**
     * Get the food category for an ingredient.
     */
    public static Category categorize(String ingredientName) {
        if (ingredientName == null) return Category.UNKNOWN;
        String key = ingredientName.toLowerCase(Locale.ROOT).trim();
        Category exact = INGREDIENT_CATEGORIES.get(key);
        if (exact != null) return exact;

        // Fuzzy match
        for (Map.Entry<String, Category> entry : INGREDIENT_CATEGORIES.entrySet()) {
            if (key.contains(entry.getKey())) return entry.getValue();
        }
        return Category.UNKNOWN;
    }

    /**
     * Get swap alternatives for an ingredient.
     * Returns items in the same category + cross-category protein swaps.
     * Excludes the current ingredient from the list.
     */
    public static List<SwapOption> getAlternatives(String ingredientName) {
        if (ingredientName == null) return List.of();
        Category cat = categorize(ingredientName);
        if (cat == Category.UNKNOWN) return List.of();

        String key = ingredientName.toLowerCase(Locale.ROOT).trim();
        List<SwapOption> alternatives = new ArrayList<>();

        // Same-category items (1:1 ratio)
        for (Map.Entry<String, Category> entry : INGREDIENT_CATEGORIES.entrySet()) {
            if (entry.getValue() == cat && !entry.getKey().equals(key)
                    && !isVariantOf(entry.getKey(), key)) {
                alternatives.add(new SwapOption(
                        entry.getKey(), capitalize(entry.getKey()), 1.0, "Same category"));
            }
        }

        // Cross-category swaps
        List<SwapOption> crossSwaps = SWAP_OPTIONS.getOrDefault(cat, List.of());
        for (SwapOption option : crossSwaps) {
            if (!option.name().equals(key) && !isVariantOf(option.name(), key)) {
                alternatives.add(option);
            }
        }

        // Deduplicate by name
        Set<String> seen = new HashSet<>();
        return alternatives.stream()
                .filter(a -> seen.add(a.name()))
                .collect(Collectors.toList());
    }

    /**
     * Check if two ingredient names are variants of each other
     * (e.g., "chicken thigh" and "chicken thighs")
     */
    private static boolean isVariantOf(String a, String b) {
        if (a.equals(b)) return true;
        if (a.startsWith(b) || b.startsWith(a)) return true;
        // Singular/plural
        if ((a + "s").equals(b) || (b + "s").equals(a)) return true;
        return false;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Arrays.stream(s.split(" "))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    /**
     * Convert a swap option to a JSON-friendly map.
     */
    public static List<Map<String, Object>> alternativesToMapList(List<SwapOption> alts) {
        return alts.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", a.displayName());
            m.put("canonicalName", a.name());
            m.put("quantityRatio", a.quantityRatio());
            m.put("note", a.note());
            // Include price and nutrition if available
            NutritionDatabase.NutritionPer100g nutr = NutritionDatabase.lookup(a.name());
            if (nutr != null) {
                m.put("caloriesPer100g", nutr.calories());
                m.put("proteinPer100g", nutr.proteinG());
            }
            PriceDatabase.StorePrice price = PriceDatabase.lookup(a.name());
            if (price != null) {
                m.put("priceCad", price.priceCad());
                m.put("priceUnit", price.perUnit());
            }
            return m;
        }).collect(Collectors.toList());
    }
}
