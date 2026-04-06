package com._6.group4.smartcart.grocery;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts recipe-level quantities (cups, grams, teaspoons) into
 * store-friendly shopping quantities (bags, heads, lbs, dozens).
 */
public final class StoreQuantityConverter {

    private StoreQuantityConverter() {}

    // Ingredients sold by the head
    private static final Set<String> HEAD_ITEMS = Set.of(
            "broccoli", "cauliflower", "cabbage", "lettuce", "romaine lettuce"
    );

    // Ingredients sold by the bag (leafy greens)
    private static final Set<String> BAG_GREENS = Set.of(
            "spinach", "kale", "mixed greens", "arugula", "spring mix"
    );

    // Ingredients sold by the bag (root veggies when counted)
    private static final Set<String> BAG_ROOT = Set.of(
            "carrot", "carrots"
    );

    // Dry goods sold by box or bag
    private static final Set<String> DRY_GOODS = Set.of(
            "rice", "brown rice", "white rice", "pasta", "whole wheat pasta",
            "quinoa", "couscous", "oats", "rolled oats", "flour",
            "sugar", "brown sugar", "whole wheat pancake mix"
    );

    // Meat and fish (convert grams to lbs)
    private static final Set<String> MEAT_FISH = Set.of(
            "chicken breast", "chicken thigh", "chicken thighs", "ground beef",
            "beef steak", "beef stew meat", "pork chop", "pork chops",
            "ground pork", "ground turkey", "ground chicken", "turkey breast",
            "salmon", "salmon fillet", "salmon fillets", "shrimp", "tilapia",
            "tuna", "cod", "bacon", "sausage"
    );

    // Dairy items with tub conversion
    private static final Set<String> YOGURT = Set.of(
            "yogurt", "plain yogurt", "greek yogurt"
    );

    // Liquid dairy
    private static final Set<String> MILK = Set.of(
            "milk", "whole milk", "2% milk", "skim milk", "cream",
            "heavy cream", "half and half"
    );

    // Packaging keywords for dry goods
    private static final Map<String, String> DRY_GOOD_PACKAGE = Map.of(
            "pasta", "box", "whole wheat pasta", "box",
            "rice", "bag", "brown rice", "bag", "white rice", "bag",
            "quinoa", "bag", "oats", "bag", "rolled oats", "bag",
            "flour", "bag", "sugar", "bag"
    );

    /**
     * Convert a recipe quantity to a store-friendly quantity string.
     *
     * @param ingredientName canonical or display name
     * @param quantity       numeric quantity (may be null)
     * @param unit           recipe unit (cup, g, count, tbsp, etc.)
     * @return store-friendly quantity string like "2 lbs" or "1 bag"
     */
    public static String convert(String ingredientName, Double quantity, String unit) {
        if (ingredientName == null) return fallback(quantity, unit);

        String name = ingredientName.toLowerCase(Locale.ROOT).trim();
        String normalizedUnit = unit != null ? unit.toLowerCase(Locale.ROOT).trim() : "";

        // Eggs — always dozens
        if (name.contains("egg")) {
            return convertEggs(quantity, normalizedUnit);
        }

        // Meat/fish — grams to lbs, count stays as-is
        if (isMeatFish(name)) {
            return convertMeat(quantity, normalizedUnit);
        }

        // Yogurt — cups to tubs
        if (isYogurt(name)) {
            return convertYogurt(quantity, normalizedUnit);
        }

        // Milk/cream — cups to cartons
        if (isMilk(name)) {
            return convertMilk(quantity, normalizedUnit);
        }

        // Broccoli, cauliflower, cabbage — cups to heads
        if (isHeadItem(name)) {
            return convertHeadItem(quantity, normalizedUnit);
        }

        // Leafy greens — cups to bags
        if (isBagGreen(name)) {
            return convertBagGreen(quantity, normalizedUnit);
        }

        // Carrots — count to bags
        if (isBagRoot(name) && isCountUnit(normalizedUnit)) {
            return convertBagRoot(quantity);
        }

        // Dry goods — cups/grams to bags/boxes
        if (isDryGood(name)) {
            return convertDryGood(name, quantity, normalizedUnit);
        }

        // Fallback — pass through original
        return fallback(quantity, unit);
    }

    private static String convertEggs(Double quantity, String unit) {
        if (quantity == null) return "1 dozen";
        if (isCountUnit(unit) || unit.isEmpty()) {
            int count = (int) Math.ceil(quantity);
            if (count <= 6) return "½ dozen";
            if (count <= 12) return "1 dozen";
            int dozens = (int) Math.ceil(count / 12.0);
            return dozens + " dozen";
        }
        return fallback(quantity, unit);
    }

    private static String convertMeat(Double quantity, String unit) {
        if (quantity == null) return fallback(quantity, unit);
        if (unit.equals("g") || unit.equals("gram") || unit.equals("grams")) {
            double lbs = quantity / 454.0;
            if (lbs <= 0.75) return "1 lb";
            return (int) Math.ceil(lbs) + " lbs";
        }
        if (unit.equals("kg") || unit.equals("kilogram")) {
            double lbs = quantity * 2.205;
            return (int) Math.ceil(lbs) + " lbs";
        }
        if (isCountUnit(unit)) {
            int count = (int) Math.ceil(quantity);
            return count + (count == 1 ? " piece" : " pieces");
        }
        return fallback(quantity, unit);
    }

    private static String convertYogurt(Double quantity, String unit) {
        if (quantity == null) return "1 tub";
        if (unit.equals("cup") || unit.equals("cups")) {
            int tubs = (int) Math.ceil(quantity / 4.0);
            return tubs + (tubs == 1 ? " tub" : " tubs");
        }
        return fallback(quantity, unit);
    }

    private static String convertMilk(Double quantity, String unit) {
        if (quantity == null) return "1 carton";
        if (unit.equals("cup") || unit.equals("cups")) {
            int cartons = (int) Math.ceil(quantity / 4.0);
            return cartons + (cartons == 1 ? " carton" : " cartons");
        }
        if (unit.equals("tbsp") || unit.equals("tablespoon")) {
            return "1 carton"; // small amount, still need to buy one
        }
        return fallback(quantity, unit);
    }

    private static String convertHeadItem(Double quantity, String unit) {
        if (quantity == null) return "1 head";
        if (unit.equals("cup") || unit.equals("cups")) {
            int heads = (int) Math.ceil(quantity / 4.0);
            return heads + (heads == 1 ? " head" : " heads");
        }
        if (isCountUnit(unit)) {
            int count = (int) Math.ceil(quantity);
            return count + (count == 1 ? " head" : " heads");
        }
        return fallback(quantity, unit);
    }

    private static String convertBagGreen(Double quantity, String unit) {
        if (quantity == null) return "1 bag";
        if (unit.equals("cup") || unit.equals("cups")) {
            int bags = (int) Math.ceil(quantity / 6.0);
            return bags + (bags == 1 ? " bag" : " bags");
        }
        return fallback(quantity, unit);
    }

    private static String convertBagRoot(Double quantity) {
        if (quantity == null) return "1 bag";
        int count = (int) Math.ceil(quantity);
        if (count <= 8) return "1 bag";
        return (int) Math.ceil(count / 8.0) + " bags";
    }

    private static String convertDryGood(String name, Double quantity, String unit) {
        String pkg = "bag";
        for (Map.Entry<String, String> entry : DRY_GOOD_PACKAGE.entrySet()) {
            if (name.contains(entry.getKey())) {
                pkg = entry.getValue();
                break;
            }
        }

        if (quantity == null) return "1 " + pkg;
        if (unit.equals("cup") || unit.equals("cups")) {
            int pkgs = (int) Math.ceil(quantity / 3.0); // ~3 cups per standard bag/box
            return pkgs + " " + pkg + (pkgs > 1 ? (pkg.equals("box") ? "es" : "s") : "");
        }
        if (unit.equals("g") || unit.equals("gram") || unit.equals("grams")) {
            int pkgs = (int) Math.ceil(quantity / 500.0);
            return pkgs + " " + pkg + (pkgs > 1 ? (pkg.equals("box") ? "es" : "s") : "");
        }
        return "1 " + pkg;
    }

    private static String fallback(Double quantity, String unit) {
        if (quantity == null && (unit == null || unit.isBlank())) return "";
        StringBuilder sb = new StringBuilder();
        if (quantity != null) {
            if (quantity == Math.floor(quantity)) {
                sb.append((int) quantity.doubleValue());
            } else {
                sb.append(String.format("%.1f", quantity));
            }
        }
        if (unit != null && !unit.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(unit);
        }
        return sb.toString();
    }

    private static boolean isMeatFish(String name) {
        return MEAT_FISH.stream().anyMatch(name::contains);
    }

    private static boolean isYogurt(String name) {
        return YOGURT.stream().anyMatch(name::contains);
    }

    private static boolean isMilk(String name) {
        return MILK.stream().anyMatch(name::contains);
    }

    private static boolean isHeadItem(String name) {
        return HEAD_ITEMS.stream().anyMatch(name::contains);
    }

    private static boolean isBagGreen(String name) {
        return BAG_GREENS.stream().anyMatch(name::contains);
    }

    private static boolean isBagRoot(String name) {
        return BAG_ROOT.stream().anyMatch(name::contains);
    }

    private static boolean isDryGood(String name) {
        return DRY_GOODS.stream().anyMatch(name::contains);
    }

    private static boolean isCountUnit(String unit) {
        return unit.isEmpty() || unit.equals("count") || unit.equals("piece")
                || unit.equals("pieces") || unit.equals("whole") || unit.equals("large")
                || unit.equals("medium") || unit.equals("small");
    }
}
