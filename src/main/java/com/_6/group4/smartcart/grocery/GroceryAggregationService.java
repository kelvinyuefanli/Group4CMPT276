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
import java.util.Objects;
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

        PantryCoverage pantryCoverage = PantryCoverage.from(pantryItems);
        List<GroceryListItemView> remainingItems = new ArrayList<>();
        List<GroceryListItemView> coveredItems = new ArrayList<>();
        int pantryAdjustedCount = 0;

        for (GroceryBucket bucket : buckets.values()) {
            GroceryListItemView view = bucket.applyPantry(pantryCoverage);
            if (view.pantryQuantityValue() != null || pantryCoverage.booleanCoverage.contains(bucket.canonicalName)) {
                pantryAdjustedCount++;
            }
            if (view.covered()) {
                coveredItems.add(view);
            } else {
                remainingItems.add(view);
            }
        }

        boolean allCoveredByPantry = !buckets.isEmpty() && remainingItems.isEmpty() && !coveredItems.isEmpty();
        return new GroceryListResponse(remainingItems, coveredItems, pantryAdjustedCount, allCoveredByPantry);
    }

    public static String normalizeStoredUnit(String rawUnit) {
        UnitInfo unitInfo = UnitInfo.from(rawUnit);
        return unitInfo != null ? unitInfo.displayUnit : null;
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
            buckets.put(key, GroceryBucket.create(key, displayName, canonicalName, quantity, unitInfo));
            return;
        }

        existing.merge(quantity, unitInfo);
    }

    public record GroceryListItemView(
            String itemKey,
            String name,
            String canonicalName,
            String quantity,
            Double quantityValue,
            String unit,
            Double pantryQuantityValue,
            boolean covered,
            String inputMode,
            String category
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("itemKey", itemKey);
            map.put("name", name);
            map.put("canonicalName", canonicalName);
            map.put("quantity", quantity);
            map.put("quantityValue", quantityValue);
            map.put("unit", unit);
            map.put("pantryQuantityValue", pantryQuantityValue);
            map.put("covered", covered);
            map.put("inputMode", inputMode);
            map.put("category", category);
            return map;
        }
    }

    public record GroceryListResponse(
            List<GroceryListItemView> items,
            List<GroceryListItemView> coveredItems,
            int pantrySubtractedCount,
            boolean allCoveredByPantry
    ) {
        static GroceryListResponse empty() {
            return new GroceryListResponse(List.of(), List.of(), 0, false);
        }

        public Map<String, Object> toResponseMap() {
            List<Map<String, Object>> serializedItems = new ArrayList<>();
            for (GroceryListItemView item : items) {
                serializedItems.add(item.toMap());
            }

            List<Map<String, Object>> serializedCoveredItems = new ArrayList<>();
            for (GroceryListItemView item : coveredItems) {
                serializedCoveredItems.add(item.toMap());
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("items", serializedItems);
            response.put("coveredItems", serializedCoveredItems);
            response.put("pantrySubtractedCount", pantrySubtractedCount);
            response.put("allCoveredByPantry", allCoveredByPantry);
            return response;
        }
    }

    private record BucketKey(String canonicalName, String bucketKey, boolean quantityKnown) {
        String itemKey() {
            return canonicalName + "|" + bucketKey + "|" + (quantityKnown ? "number" : "toggle");
        }
    }

    private static final class GroceryBucket {
        private final BucketKey key;
        private final String name;
        private final String canonicalName;
        private final String category;
        private final UnitInfo displayUnitInfo;
        private final boolean quantityKnown;
        private Double quantity;

        private GroceryBucket(BucketKey key,
                              String name,
                              String canonicalName,
                              String category,
                              Double quantity,
                              UnitInfo displayUnitInfo,
                              boolean quantityKnown) {
            this.key = key;
            this.name = name;
            this.canonicalName = canonicalName;
            this.category = category;
            this.quantity = quantity;
            this.displayUnitInfo = displayUnitInfo;
            this.quantityKnown = quantityKnown;
        }

        static GroceryBucket create(BucketKey key, String name, String canonicalName, Double quantity, UnitInfo unitInfo) {
            return new GroceryBucket(
                    key,
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

        GroceryListItemView applyPantry(PantryCoverage pantryCoverage) {
            boolean booleanCovered = pantryCoverage.booleanCoverage.contains(canonicalName);
            if (!quantityKnown) {
                return new GroceryListItemView(
                        key.itemKey(),
                        name,
                        canonicalName,
                        displayUnitInfo != null ? displayUnitInfo.displayUnit : "",
                        null,
                        displayUnitInfo != null ? displayUnitInfo.displayUnit : null,
                        null,
                        booleanCovered,
                        "toggle",
                        category
                );
            }

            PantryQuantityBucket pantryBucket = pantryCoverage.numericCoverage.get(key);
            Double pantryQuantityValue = null;
            if (pantryBucket != null && quantity != null) {
                pantryQuantityValue = pantryBucket.asDisplayQuantity(displayUnitInfo);
            }

            boolean covered = booleanCovered;
            Double remainingQuantity = quantity;
            if (covered && remainingQuantity != null) {
                remainingQuantity = 0d;
            }
            if (!covered && quantity != null && pantryQuantityValue != null) {
                remainingQuantity = Math.max(quantity - pantryQuantityValue, 0d);
                covered = remainingQuantity == 0d;
            }

            return new GroceryListItemView(
                    key.itemKey(),
                    name,
                    canonicalName,
                    formatQuantity(remainingQuantity, displayUnitInfo != null ? displayUnitInfo.displayUnit : null),
                    remainingQuantity,
                    displayUnitInfo != null ? displayUnitInfo.displayUnit : null,
                    pantryQuantityValue,
                    covered,
                    "number",
                    category
            );
        }
    }

    private static final class PantryCoverage {
        private final Set<String> booleanCoverage;
        private final Map<BucketKey, PantryQuantityBucket> numericCoverage;

        private PantryCoverage(Set<String> booleanCoverage, Map<BucketKey, PantryQuantityBucket> numericCoverage) {
            this.booleanCoverage = booleanCoverage;
            this.numericCoverage = numericCoverage;
        }

        static PantryCoverage from(Collection<PantryItem> pantryItems) {
            Set<String> booleanCoverage = new LinkedHashSet<>();
            Map<BucketKey, PantryQuantityBucket> numericCoverage = new LinkedHashMap<>();

            if (pantryItems == null) {
                return new PantryCoverage(booleanCoverage, numericCoverage);
            }

            for (PantryItem pantryItem : pantryItems) {
                String canonicalName = IngredientNormalizer.canonicalizeName(
                        pantryItem.getCanonicalName() != null && !pantryItem.getCanonicalName().isBlank()
                                ? pantryItem.getCanonicalName()
                                : pantryItem.getIngredientName()
                );
                if (canonicalName.isBlank()) {
                    continue;
                }

                if (pantryItem.getQuantity() == null) {
                    booleanCoverage.add(canonicalName);
                    continue;
                }

                UnitInfo unitInfo = UnitInfo.from(pantryItem.getUnit());
                BucketKey key = new BucketKey(
                        canonicalName,
                        unitInfo != null ? unitInfo.bucketKey : "no-unit",
                        true
                );

                PantryQuantityBucket bucket = numericCoverage.get(key);
                if (bucket == null) {
                    numericCoverage.put(key, PantryQuantityBucket.create(pantryItem.getQuantity(), unitInfo));
                } else {
                    bucket.merge(pantryItem.getQuantity(), unitInfo);
                }
            }

            return new PantryCoverage(booleanCoverage, numericCoverage);
        }
    }

    private static final class PantryQuantityBucket {
        private final UnitInfo displayUnitInfo;
        private Double quantity;

        private PantryQuantityBucket(Double quantity, UnitInfo displayUnitInfo) {
            this.quantity = quantity;
            this.displayUnitInfo = displayUnitInfo;
        }

        static PantryQuantityBucket create(Double quantity, UnitInfo displayUnitInfo) {
            return new PantryQuantityBucket(quantity, displayUnitInfo);
        }

        void merge(Double otherQuantity, UnitInfo otherUnitInfo) {
            if (quantity == null || otherQuantity == null) {
                return;
            }
            if (displayUnitInfo == null || otherUnitInfo == null) {
                quantity += otherQuantity;
                return;
            }
            quantity += otherUnitInfo.convertTo(otherQuantity, displayUnitInfo);
        }

        Double asDisplayQuantity(UnitInfo targetUnitInfo) {
            if (quantity == null) {
                return null;
            }
            if (displayUnitInfo == null || targetUnitInfo == null) {
                return quantity;
            }
            return displayUnitInfo.convertTo(quantity, targetUnitInfo);
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
            if (!Objects.equals(bucketKey, target.bucketKey)) {
                throw new IllegalArgumentException("Cannot convert " + displayUnit + " to " + target.displayUnit);
            }
            if (Objects.equals(displayUnit, target.displayUnit)) {
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
