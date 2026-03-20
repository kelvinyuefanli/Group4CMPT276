package com._6.group4.smartcart.grocery;

import com._6.group4.smartcart.mealplanning.MealPlan;
import com._6.group4.smartcart.mealplanning.MealPlanRecipe;
import com._6.group4.smartcart.mealplanning.Recipe;
import com._6.group4.smartcart.mealplanning.RecipeIngredient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GroceryAggregationService {

    public GroceryListResponse buildGroceryList(MealPlan plan, Collection<PantryItem> pantryItems) {
        if (plan == null) {
            return GroceryListResponse.empty();
        }

        Map<BucketKey, GroceryBucket> buckets = new LinkedHashMap<>();
        for (MealPlanRecipe mealPlanRecipe : plan.getRecipes()) {
            Recipe recipe = mealPlanRecipe.getRecipe();
            if (recipe == null) {
                continue;
            }
            for (RecipeIngredient ingredient : recipe.getIngredients()) {
                addIngredient(buckets, ingredient);
            }
        }

        if (buckets.isEmpty()) {
            return GroceryListResponse.empty();
        }

        Set<String> pantryCanonicalNames = new LinkedHashSet<>();
        if (pantryItems != null) {
            for (PantryItem pantryItem : pantryItems) {
                String canonical = IngredientNormalizer.canonicalizeName(pantryItem.getIngredientName());
                if (!canonical.isBlank()) {
                    pantryCanonicalNames.add(canonical);
                }
            }
        }

        List<GroceryListItemView> items = new ArrayList<>();
        Set<String> removedCanonical = new LinkedHashSet<>();
        for (GroceryBucket bucket : buckets.values()) {
            if (pantryCanonicalNames.contains(bucket.canonicalName)) {
                removedCanonical.add(bucket.canonicalName);
                continue;
            }
            items.add(bucket.toView());
        }

        boolean allCoveredByPantry = !buckets.isEmpty() && items.isEmpty();
        return new GroceryListResponse(items, removedCanonical.size(), allCoveredByPantry);
    }

    private void addIngredient(Map<BucketKey, GroceryBucket> buckets, RecipeIngredient ingredient) {
        if (ingredient == null || ingredient.getIngredientName() == null || ingredient.getIngredientName().isBlank()) {
            return;
        }

        String displayName = ingredient.getIngredientName().trim();
        String canonicalName = IngredientNormalizer.canonicalizeName(
                ingredient.getCanonicalName() != null && !ingredient.getCanonicalName().isBlank()
                        ? ingredient.getCanonicalName()
                        : ingredient.getIngredientName()
        );
        if (canonicalName.isBlank()) {
            canonicalName = IngredientNormalizer.canonicalizeName(displayName);
        }
        if (canonicalName.isBlank()) {
            return;
        }

        Double quantity = ingredient.getQuantity();
        UnitInfo unitInfo = UnitInfo.from(ingredient.getUnit());
        BucketKey key = new BucketKey(
                canonicalName,
                unitInfo != null ? unitInfo.bucketKey : "no-unit",
                quantity != null
        );

        GroceryBucket existing = buckets.get(key);
        if (existing == null) {
            buckets.put(key, GroceryBucket.create(displayName, canonicalName, quantity, unitInfo));
            return;
        }

        existing.merge(quantity, unitInfo);
    }

    public record GroceryListItemView(String name, String quantity, String category) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", name);
            map.put("quantity", quantity);
            map.put("category", category);
            return map;
        }
    }

    public record GroceryListResponse(List<GroceryListItemView> items, int pantrySubtractedCount, boolean allCoveredByPantry) {
        static GroceryListResponse empty() {
            return new GroceryListResponse(List.of(), 0, false);
        }

        public Map<String, Object> toResponseMap() {
            List<Map<String, Object>> serializedItems = new ArrayList<>();
            for (GroceryListItemView item : items) {
                serializedItems.add(item.toMap());
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("items", serializedItems);
            response.put("pantrySubtractedCount", pantrySubtractedCount);
            response.put("allCoveredByPantry", allCoveredByPantry);
            return response;
        }
    }

    private record BucketKey(String canonicalName, String bucketKey, boolean quantityKnown) {
    }

    private static final class GroceryBucket {
        private final String name;
        private final String canonicalName;
        private final String category;
        private final UnitInfo displayUnitInfo;
        private final boolean quantityKnown;
        private Double quantity;

        private GroceryBucket(String name,
                              String canonicalName,
                              String category,
                              Double quantity,
                              UnitInfo displayUnitInfo,
                              boolean quantityKnown) {
            this.name = name;
            this.canonicalName = canonicalName;
            this.category = category;
            this.quantity = quantity;
            this.displayUnitInfo = displayUnitInfo;
            this.quantityKnown = quantityKnown;
        }

        static GroceryBucket create(String name, String canonicalName, Double quantity, UnitInfo unitInfo) {
            return new GroceryBucket(
                    name,
                    canonicalName,
                    categorize(name),
                    quantity,
                    unitInfo,
                    quantity != null
            );
        }

        void merge(Double otherQuantity, UnitInfo otherUnitInfo) {
            if (!quantityKnown || quantity == null || otherQuantity == null) {
                return;
            }
            if (displayUnitInfo == null || otherUnitInfo == null) {
                quantity += otherQuantity;
                return;
            }
            quantity += otherUnitInfo.convertTo(otherQuantity, displayUnitInfo);
        }

        GroceryListItemView toView() {
            return new GroceryListItemView(
                    name,
                    formatQuantity(quantity, displayUnitInfo != null ? displayUnitInfo.displayUnit : null),
                    category
            );
        }
    }

    static String formatQuantity(Double quantity, String unit) {
        if (quantity == null) {
            return unit != null ? unit : "";
        }

        BigDecimal decimal = BigDecimal.valueOf(quantity)
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        String formatted = decimal.scale() < 0
                ? decimal.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
                : decimal.toPlainString();
        return unit != null ? formatted + " " + unit : formatted;
    }

    static String categorize(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.matches(".*(chicken|beef|salmon|turkey|pork|fish|shrimp|tofu|egg).*")) {
            return "Protein";
        }
        if (lower.matches(".*(milk|cheese|yogurt|cream|butter).*")) {
            return "Dairy";
        }
        if (lower.matches(".*(lettuce|tomato|onion|garlic|pepper|carrot|celery|spinach|avocado|lemon|lime|basil|parsley|cilantro|dill|ginger|broccoli|cabbage|cucumber).*")) {
            return "Produce";
        }
        return "Pantry";
    }

    private static final class UnitInfo {
        private final String bucketKey;
        private final String displayUnit;
        private final double baseFactor;

        private UnitInfo(String bucketKey, String displayUnit, double baseFactor) {
            this.bucketKey = bucketKey;
            this.displayUnit = displayUnit;
            this.baseFactor = baseFactor;
        }

        static UnitInfo from(String rawUnit) {
            if (rawUnit == null || rawUnit.isBlank()) {
                return null;
            }

            String unit = normalizeUnitText(rawUnit);
            return switch (unit) {
                case "tsp", "teaspoon" -> new UnitInfo("volume", "tsp", 4.92892d);
                case "tbsp", "tablespoon" -> new UnitInfo("volume", "tbsp", 14.7868d);
                case "cup" -> new UnitInfo("volume", "cup", 236.588d);
                case "ml" -> new UnitInfo("volume", "ml", 1.0d);
                case "l", "liter", "litre" -> new UnitInfo("volume", "l", 1000.0d);
                case "fl oz", "fluid ounce" -> new UnitInfo("volume", "fl oz", 29.5735d);
                case "oz", "ounce" -> new UnitInfo("weight", "oz", 28.3495d);
                case "lb", "pound" -> new UnitInfo("weight", "lb", 453.592d);
                case "g", "gram" -> new UnitInfo("weight", "g", 1.0d);
                case "kg", "kilogram" -> new UnitInfo("weight", "kg", 1000.0d);
                case "count", "each", "item", "piece" -> new UnitInfo("count", "count", 1.0d);
                default -> {
                    String normalizedExact = singularizeUnitPhrase(unit);
                    yield new UnitInfo("exact:" + normalizedExact, normalizedExact, 1.0d);
                }
            };
        }

        double convertTo(double quantity, UnitInfo target) {
            if (!bucketKey.equals(target.bucketKey)) {
                throw new IllegalArgumentException("Cannot convert " + displayUnit + " to " + target.displayUnit);
            }
            if (displayUnit.equals(target.displayUnit)) {
                return quantity;
            }
            double baseQuantity = quantity * baseFactor;
            return baseQuantity / target.baseFactor;
        }

        private static String normalizeUnitText(String rawUnit) {
            String normalized = rawUnit.toLowerCase(Locale.ROOT).trim();
            normalized = normalized.replace('_', ' ').replace('-', ' ');
            normalized = normalized.replaceAll("\\.", "");
            normalized = normalized.replaceAll("\\s+", " ");

            return switch (normalized) {
                case "tsp", "tsps", "teaspoon", "teaspoons" -> "tsp";
                case "tbsp", "tbsps", "tablespoon", "tablespoons" -> "tbsp";
                case "cup", "cups" -> "cup";
                case "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> "ml";
                case "l", "liter", "liters", "litre", "litres" -> "l";
                case "floz", "fl oz", "fluid ounce", "fluid ounces" -> "fl oz";
                case "oz", "ounce", "ounces" -> "oz";
                case "lb", "lbs", "pound", "pounds" -> "lb";
                case "g", "gram", "grams" -> "g";
                case "kg", "kilogram", "kilograms" -> "kg";
                case "ea", "each", "item", "items", "piece", "pieces", "count" -> "count";
                default -> normalized;
            };
        }

        private static String singularizeUnitPhrase(String unit) {
            String[] parts = unit.split(" ");
            for (int i = 0; i < parts.length; i++) {
                String token = parts[i];
                if (token.endsWith("ies") && token.length() > 4) {
                    parts[i] = token.substring(0, token.length() - 3) + "y";
                } else if (token.endsWith("es") && token.length() > 3 && !token.endsWith("ses")) {
                    parts[i] = token.substring(0, token.length() - 2);
                } else if (token.endsWith("s") && token.length() > 3 && !token.endsWith("ss")) {
                    parts[i] = token.substring(0, token.length() - 1);
                }
            }
            return String.join(" ", parts);
        }
    }
}
