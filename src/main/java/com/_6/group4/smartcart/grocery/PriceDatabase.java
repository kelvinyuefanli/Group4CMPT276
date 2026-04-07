package com._6.group4.smartcart.grocery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static Canadian grocery price table.
 * Prices are per standard store purchase unit (per lb, per head, per bag, per dozen, etc.)
 * Based on average BC grocery prices (Save-On-Foods, Real Canadian Superstore, No Frills).
 * Last updated: March 2026.
 */
public final class PriceDatabase {

    private PriceDatabase() {}

    /** Price in CAD per store purchase unit. */
    public record StorePrice(double priceCad, String perUnit) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("priceCad", Math.round(priceCad * 100.0) / 100.0);
            m.put("perUnit", perUnit);
            return m;
        }
    }

    public record GroceryEstimate(String name, String storeQuantity, double itemCost) {}

    public record WeeklyBudgetEstimate(double totalCost, List<GroceryEstimate> items,
                                        double recommendedMinimum, String budgetStatus) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("totalCost", Math.round(totalCost * 100.0) / 100.0);
            m.put("recommendedMinimum", Math.round(recommendedMinimum * 100.0) / 100.0);
            m.put("budgetStatus", budgetStatus);
            var itemMaps = items.stream().map(i -> {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("name", i.name());
                im.put("storeQuantity", i.storeQuantity());
                im.put("itemCost", Math.round(i.itemCost() * 100.0) / 100.0);
                return im;
            }).toList();
            m.put("items", itemMaps);
            return m;
        }
    }

    // ---- Canadian grocery prices (CAD) ----
    private static final Map<String, StorePrice> DB = new LinkedHashMap<>();
    static {
        // Poultry (per lb)
        put("chicken breast", 6.99, "lb");
        put("chicken thigh", 4.49, "lb");
        put("chicken thighs", 4.49, "lb");
        put("ground chicken", 5.99, "lb");
        put("turkey breast", 7.49, "lb");
        put("ground turkey", 6.49, "lb");

        // Beef & Pork (per lb)
        put("ground beef", 5.99, "lb");
        put("beef steak", 12.99, "lb");
        put("beef stew meat", 8.99, "lb");
        put("pork chops", 4.99, "lb");
        put("ground pork", 4.49, "lb");
        put("bacon", 7.99, "pack");  // 375g pack
        put("sausage", 5.99, "pack"); // 450g pack

        // Seafood (per lb or per fillet)
        put("salmon", 12.99, "lb");
        put("salmon fillet", 12.99, "lb");
        put("shrimp", 9.99, "lb");
        put("tilapia", 7.99, "lb");
        put("tuna", 10.99, "lb");
        put("cod", 9.49, "lb");

        // Eggs & Dairy
        put("egg", 4.49, "dozen");
        put("eggs", 4.49, "dozen");
        put("large eggs", 4.49, "dozen");
        put("milk", 5.49, "carton");  // 2L carton
        put("yogurt", 5.99, "tub");   // 650g tub
        put("plain yogurt", 5.99, "tub");
        put("greek yogurt", 6.49, "tub");
        put("butter", 5.99, "block"); // 454g block
        put("cheese", 6.99, "block"); // 400g block
        put("cream cheese", 4.49, "block"); // 250g

        // Grains & Pasta
        put("rice", 4.99, "bag");           // 2kg bag
        put("brown rice", 5.49, "bag");
        put("white rice", 4.99, "bag");
        put("pasta", 2.49, "box");          // 450g box
        put("whole wheat pasta", 2.99, "box");
        put("quinoa", 6.99, "bag");         // 900g bag
        put("oats", 4.99, "bag");           // 1kg bag
        put("rolled oats", 4.99, "bag");
        put("bread", 3.49, "loaf");
        put("whole wheat bread", 3.99, "loaf");
        put("tortilla", 4.49, "pack");      // 10-pack
        put("whole wheat tortilla", 4.99, "pack");
        put("couscous", 3.99, "box");
        put("pita bread", 3.49, "pack");    // 6-pack
        put("whole wheat pita", 3.99, "pack");
        put("pancake mix", 4.49, "box");

        // Vegetables
        put("broccoli", 2.99, "head");
        put("spinach", 3.99, "bag");        // 312g bag
        put("kale", 3.49, "bag");
        put("romaine lettuce", 2.99, "head");
        put("mixed greens", 4.49, "bag");   // 142g clamshell
        put("cabbage", 2.49, "head");
        put("cauliflower", 3.99, "head");
        put("brussels sprouts", 4.49, "bag");
        put("bell pepper", 1.49, "each");
        put("bell peppers", 1.49, "each");
        put("zucchini", 1.29, "each");
        put("carrot", 2.99, "bag");         // 2lb bag
        put("carrots", 2.99, "bag");
        put("tomato", 0.99, "each");
        put("tomatoes", 0.99, "each");
        put("cucumber", 1.29, "each");
        put("green beans", 3.49, "bag");    // 340g bag
        put("corn", 0.79, "each");          // per cob
        put("peas", 2.49, "bag");           // frozen 500g
        put("sweet potato", 1.49, "each");
        put("sweet potatoes", 1.49, "each");
        put("potato", 4.99, "bag");         // 5lb bag
        put("potatoes", 4.99, "bag");
        put("butternut squash", 3.49, "each");
        put("beets", 2.99, "bunch");
        put("mushroom", 3.49, "pack");      // 227g pack
        put("mushrooms", 3.49, "pack");
        put("asparagus", 4.49, "bunch");
        put("celery", 2.49, "bunch");
        put("eggplant", 2.99, "each");
        put("onion", 0.99, "each");
        put("onions", 2.99, "bag");         // 3lb bag
        put("garlic", 0.99, "head");

        // Fruits
        put("banana", 0.29, "each");
        put("bananas", 0.29, "each");
        put("apple", 0.99, "each");
        put("apples", 0.99, "each");
        put("berries", 4.99, "pack");       // 170g clamshell
        put("mixed berries", 4.99, "pack");
        put("strawberries", 4.99, "pack");  // 454g pack
        put("blueberries", 4.99, "pack");   // 170g pack
        put("orange", 0.99, "each");
        put("oranges", 0.99, "each");
        put("avocado", 1.99, "each");
        put("mango", 1.49, "each");
        put("lemon", 0.69, "each");
        put("lemons", 0.69, "each");
        put("lime", 0.49, "each");
        put("limes", 0.49, "each");
        put("grapes", 3.99, "bag");
        put("watermelon", 6.99, "each");
        put("peaches", 0.99, "each");
        put("pears", 0.99, "each");
        put("pineapple", 3.99, "each");

        // Legumes
        put("black beans", 1.29, "can");
        put("chickpeas", 1.29, "can");
        put("lentils", 2.49, "bag");        // 900g bag
        put("tofu", 2.99, "block");         // 350g block
        put("tempeh", 4.49, "pack");

        // Oils & Condiments (already in pantry usually, but priced for reference)
        put("olive oil", 8.99, "bottle");   // 500ml
        put("vegetable oil", 4.99, "bottle");
        put("soy sauce", 3.49, "bottle");
        put("vinegar", 2.99, "bottle");
        put("honey", 7.99, "jar");
        put("ketchup", 3.99, "bottle");
        put("mustard", 2.99, "bottle");
        put("mayonnaise", 4.99, "jar");
        put("hot sauce", 3.49, "bottle");

        // Nuts
        put("peanut butter", 5.99, "jar");
        put("almonds", 8.99, "bag");
        put("walnuts", 9.99, "bag");
    }

    private static void put(String name, double price, String perUnit) {
        DB.put(name.toLowerCase(Locale.ROOT), new StorePrice(price, perUnit));
    }

    /**
     * Look up price for an ingredient by name.
     * Returns null if not found.
     */
    public static StorePrice lookup(String ingredientName) {
        if (ingredientName == null) return null;
        String key = ingredientName.toLowerCase(Locale.ROOT).trim();
        StorePrice exact = DB.get(key);
        if (exact != null) return exact;

        // Fuzzy match
        for (Map.Entry<String, StorePrice> entry : DB.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Estimate the cost of a grocery list item based on its store quantity.
     * Used for grocery list totals (buying whole units at the store).
     */
    public static double estimateItemCost(String ingredientName, String storeQuantity) {
        StorePrice price = lookup(ingredientName);
        if (price == null) return 0;

        int units = parseStoreQuantityCount(storeQuantity);
        return price.priceCad() * units;
    }

    /**
     * Estimate the proportional cost of an ingredient in a single recipe.
     * E.g., 200g chicken breast = (200/454) * $6.99/lb = $3.08
     * Used for per-recipe cost display (not grocery list).
     */
    public static double estimateIngredientCost(String ingredientName, Double quantity, String unit) {
        if (ingredientName == null || quantity == null || quantity <= 0) return 0;
        StorePrice price = lookup(ingredientName);
        if (price == null) return 0;

        String n = ingredientName.toLowerCase(Locale.ROOT).trim();
        String u = unit != null ? unit.toLowerCase(Locale.ROOT).trim() : "";

        // Skip pantry staples (salt, pepper, oil "to taste")
        if (u.equals("to taste") || u.contains("to taste") || u.contains("pinch")
                || u.contains("dash") || quantity < 0.01) return 0;

        double fractionOfUnit = estimateFraction(n, quantity, u, price);
        return price.priceCad() * fractionOfUnit;
    }

    private static double estimateFraction(String name, double qty, String unit, StorePrice price) {
        String perUnit = price.perUnit();

        // Price is per lb — convert ingredient to lbs
        if (perUnit.equals("lb")) {
            double lbs;
            if (unit.equals("g") || unit.equals("gram") || unit.equals("grams")) {
                lbs = qty / 454.0;
            } else if (unit.equals("kg")) {
                lbs = qty * 2.205;
            } else if (unit.equals("lb") || unit.equals("lbs") || unit.equals("pound")) {
                lbs = qty;
            } else if (unit.equals("oz") || unit.equals("ounce")) {
                lbs = qty / 16.0;
            } else {
                // Count-based (pieces) — estimate weight per piece
                double gramsPerPiece = estimateGramsPerPiece(name);
                lbs = (qty * gramsPerPiece) / 454.0;
            }
            return lbs;
        }

        // Price is per dozen
        if (perUnit.equals("dozen")) {
            return qty / 12.0;
        }

        // Price is per each
        if (perUnit.equals("each")) {
            if (unit.equals("cup") || unit.equals("cups")) {
                return qty * 0.75; // 1 cup chopped ≈ 0.75 of a whole piece
            }
            // Cloves of garlic: ~10 cloves per head
            if (unit.equals("clove") || unit.equals("cloves")) {
                return qty / 10.0;
            }
            // Slices: fraction of a whole
            if (unit.equals("slice") || unit.equals("slices")) {
                return qty / 12.0;
            }
            // Size descriptors: these are pieces, not multipliers
            if (unit.equals("medium") || unit.equals("small") || unit.equals("large")) {
                return qty;
            }
            // tbsp/tsp of something priced per each (e.g., lemon juice)
            if (unit.equals("tbsp") || unit.equals("tablespoon")) {
                return qty / 4.0; // ~4 tbsp per lemon/lime
            }
            if (unit.equals("tsp") || unit.equals("teaspoon")) {
                return qty / 12.0;
            }
            return qty;
        }

        // Price is per bag/box/tub/carton/pack — estimate fraction used
        if (perUnit.equals("bag") || perUnit.equals("box") || perUnit.equals("tub")
                || perUnit.equals("carton") || perUnit.equals("pack")
                || perUnit.equals("block") || perUnit.equals("loaf")
                || perUnit.equals("bottle") || perUnit.equals("jar")
                || perUnit.equals("bunch")) {
            if (unit.equals("cup") || unit.equals("cups")) {
                // Rough: most bags/boxes contain ~3-4 cups
                return qty / 3.5;
            }
            if (unit.equals("tbsp") || unit.equals("tablespoon")) {
                return qty / 32.0; // ~32 tbsp per standard container
            }
            if (unit.equals("tsp") || unit.equals("teaspoon")) {
                return qty / 96.0; // ~96 tsp per standard container
            }
            if (unit.equals("g") || unit.equals("gram") || unit.equals("grams")) {
                return qty / 500.0; // assume 500g standard package
            }
            if (unit.equals("slice") || unit.equals("slices")) {
                return qty / 20.0; // ~20 slices per loaf
            }
            // Count of items from a pack
            return Math.min(qty / 8.0, 1.0); // assume 8 per pack
        }

        // Default: assume 1 unit
        return Math.min(qty, 1.0);
    }

    private static double estimateGramsPerPiece(String name) {
        if (name.contains("chicken")) return 180;
        if (name.contains("salmon") || name.contains("fillet")) return 170;
        if (name.contains("pork")) return 200;
        if (name.contains("beef")) return 200;
        return 150; // default
    }

    /**
     * Estimate weekly grocery cost from a list of items with store quantities.
     * Also calculates recommended minimum budget based on serving size.
     */
    public static WeeklyBudgetEstimate estimateWeeklyCost(
            List<GroceryItemInput> items, int servingSize) {

        double totalCost = 0;
        List<GroceryEstimate> estimates = new java.util.ArrayList<>();

        for (GroceryItemInput item : items) {
            double cost = estimateItemCost(item.name(), item.storeQuantity());
            if (cost > 0) {
                estimates.add(new GroceryEstimate(item.name(), item.storeQuantity(), cost));
            }
            totalCost += cost;
        }

        // Sort by cost descending (most expensive first)
        estimates.sort((a, b) -> Double.compare(b.itemCost(), a.itemCost()));

        // Recommended minimum: $40/person/week (Canadian average for basic meals)
        double recommendedMinimum = servingSize * 40.0;

        String status;
        if (totalCost <= recommendedMinimum * 0.8) {
            status = "under_budget";
        } else if (totalCost <= recommendedMinimum * 1.2) {
            status = "on_budget";
        } else {
            status = "over_budget";
        }

        return new WeeklyBudgetEstimate(totalCost, estimates, recommendedMinimum, status);
    }

    public record GroceryItemInput(String name, String storeQuantity) {}

    /**
     * Parse the count from a store quantity string like "2 lbs", "1 dozen", "3 bags".
     * Returns 1 if unparseable.
     */
    static int parseStoreQuantityCount(String storeQuantity) {
        if (storeQuantity == null || storeQuantity.isBlank()) return 1;
        String s = storeQuantity.trim();

        // Handle fractions
        if (s.startsWith("½")) return 1; // half → round up to 1

        // Extract leading number
        StringBuilder numStr = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                numStr.append(c);
            } else {
                break;
            }
        }

        if (numStr.length() == 0) return 1;
        try {
            double val = Double.parseDouble(numStr.toString());
            return Math.max(1, (int) Math.ceil(val));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
