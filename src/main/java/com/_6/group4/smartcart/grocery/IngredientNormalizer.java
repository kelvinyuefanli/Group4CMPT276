package com._6.group4.smartcart.grocery;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class IngredientNormalizer {

    private static final Map<String, String> EXACT_ALIASES = Map.ofEntries(
            // Onion variants
            Map.entry("scallion", "green onion"),
            Map.entry("scallions", "green onion"),
            Map.entry("spring onion", "green onion"),
            Map.entry("spring onions", "green onion"),
            Map.entry("green onions", "green onion"),
            // Legume variants
            Map.entry("garbanzo bean", "chickpea"),
            Map.entry("garbanzo beans", "chickpea"),
            Map.entry("chickpeas", "chickpea"),
            // Chicken thigh variants → single canonical name
            Map.entry("chicken thigh", "chicken thigh"),
            Map.entry("chicken thighs", "chicken thigh"),
            Map.entry("boneless skinless chicken thigh", "chicken thigh"),
            Map.entry("boneless skinless chicken thighs", "chicken thigh"),
            Map.entry("bone in skin on chicken thigh", "chicken thigh"),
            Map.entry("bone in chicken thigh", "chicken thigh"),
            // Chicken breast variants
            Map.entry("chicken breast", "chicken breast"),
            Map.entry("chicken breasts", "chicken breast"),
            Map.entry("boneless skinless chicken breast", "chicken breast"),
            Map.entry("boneless skinless chicken breasts", "chicken breast"),
            // Egg variants
            Map.entry("egg", "egg"),
            Map.entry("eggs", "egg"),
            Map.entry("large egg", "egg"),
            Map.entry("large eggs", "egg"),
            // Salmon variants
            Map.entry("salmon fillet", "salmon"),
            Map.entry("salmon fillets", "salmon"),
            // Potato variants
            Map.entry("potato", "potato"),
            Map.entry("potatoes", "potato"),
            Map.entry("medium potato", "potato"),
            Map.entry("small potato", "potato"),
            Map.entry("small potatoes", "potato"),
            // Bell pepper variants
            Map.entry("bell pepper", "bell pepper"),
            Map.entry("bell peppers", "bell pepper"),
            Map.entry("red bell pepper", "bell pepper"),
            Map.entry("green bell pepper", "bell pepper"),
            Map.entry("yellow bell pepper", "bell pepper")
    );

    private static final Set<String> PROTECTED_TOKENS = Set.of(
            "asparagus",
            "basil",
            "beef",
            "broccoli",
            "celery",
            "cheese",
            "cilantro",
            "corn",
            "couscous",
            "cream",
            "dill",
            "fish",
            "fruit",
            "garlic",
            "ginger",
            "hummus",
            "kale",
            "lettuce",
            "milk",
            "molasses",
            "oats",
            "oregano",
            "pasta",
            "parsley",
            "pork",
            "quinoa",
            "rice",
            "rosemary",
            "sage",
            "salmon",
            "shrimp",
            "spinach",
            "thyme",
            "tofu",
            "turkey",
            "watercress",
            "yogurt"
    );

    private IngredientNormalizer() {
    }

    /** Descriptive prefixes that should be stripped before canonicalization. */
    private static final Set<String> DESCRIPTOR_WORDS = Set.of(
            "boneless", "skinless", "bone", "in", "skin", "on",
            "fresh", "frozen", "dried", "raw", "cooked", "canned",
            "large", "medium", "small", "extra", "thin", "thick",
            "organic", "unsalted", "salted", "whole", "chopped",
            "diced", "minced", "sliced", "shredded", "grated",
            "ground", "crushed", "dry", "uncooked"
    );

    /** Words that indicate the item IS the protein (don't strip these). */
    private static final Set<String> CORE_FOOD_WORDS = Set.of(
            "chicken", "beef", "pork", "turkey", "salmon", "shrimp", "tofu",
            "egg", "rice", "pasta", "bread", "potato", "onion", "garlic",
            "pepper", "peppers", "tomato", "tomatoes", "spinach", "broccoli", "carrot", "carrots", "bean", "beans",
            "onions", "eggs", "potatoes", "thighs", "breasts", "fillets",
            "lentil", "yogurt", "milk", "cheese", "butter", "oil",
            "flour", "sugar", "salt", "thigh", "breast", "fillet"
    );

    public static String canonicalizeName(String raw) {
        if (raw == null) {
            return "";
        }

        String cleaned = raw.toLowerCase(Locale.ROOT).trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        cleaned = cleaned.replace('_', ' ').replace('-', ' ');
        cleaned = cleaned.replaceAll("\\([^)]*\\)", " ");

        // Replace commas with spaces instead of truncating — handles
        // "Boneless, Skinless Chicken Thighs" correctly
        cleaned = cleaned.replace(',', ' ');

        cleaned = cleaned.replaceAll("[^a-z0-9\\s]", " ");
        cleaned = collapseWhitespace(cleaned);

        // Strip descriptor words first, then alias, then singularize
        cleaned = stripDescriptors(cleaned);
        cleaned = collapseWhitespace(cleaned);
        cleaned = applyAlias(cleaned);
        cleaned = singularizePhrase(cleaned);
        cleaned = collapseWhitespace(cleaned);
        return applyAlias(cleaned);
    }

    /**
     * Removes common descriptor words (boneless, skinless, fresh, etc.)
     * while preserving core food words.
     */
    private static String stripDescriptors(String phrase) {
        String[] words = phrase.split("\\s+");
        // Only strip if there are food words present — don't reduce to empty
        boolean hasFoodWord = false;
        for (String w : words) {
            if (CORE_FOOD_WORDS.contains(w)) { hasFoodWord = true; break; }
        }
        if (!hasFoodWord) return phrase;

        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!DESCRIPTOR_WORDS.contains(w)) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(w);
            }
        }
        return sb.isEmpty() ? phrase : sb.toString();
    }

    static String collapseWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String applyAlias(String cleaned) {
        return EXACT_ALIASES.getOrDefault(cleaned, cleaned);
    }

    private static String singularizePhrase(String value) {
        if (value.isBlank()) {
            return value;
        }

        String[] parts = value.split(" ");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = singularizeToken(parts[i]);
        }
        return String.join(" ", parts);
    }

    private static String singularizeToken(String token) {
        if (token.length() <= 3 || PROTECTED_TOKENS.contains(token) || token.endsWith("ss")) {
            return token;
        }
        if (token.endsWith("ies") && token.length() > 4) {
            return token.substring(0, token.length() - 3) + "y";
        }
        if (token.endsWith("oes") && token.length() > 4) {
            return token.substring(0, token.length() - 2);
        }
        if (token.endsWith("ches") || token.endsWith("shes")
                || token.endsWith("xes") || token.endsWith("zes")) {
            return token.substring(0, token.length() - 2);
        }
        if (token.endsWith("ses") && !token.endsWith("sses") && token.length() > 4) {
            return token.substring(0, token.length() - 2);
        }
        if (token.endsWith("s")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }
}
