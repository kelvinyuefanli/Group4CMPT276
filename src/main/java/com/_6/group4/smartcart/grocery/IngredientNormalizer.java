package com._6.group4.smartcart.grocery;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class IngredientNormalizer {

    private static final Map<String, String> EXACT_ALIASES = Map.ofEntries(
            Map.entry("scallion", "green onion"),
            Map.entry("scallions", "green onion"),
            Map.entry("spring onion", "green onion"),
            Map.entry("spring onions", "green onion"),
            Map.entry("green onions", "green onion"),
            Map.entry("garbanzo bean", "chickpea"),
            Map.entry("garbanzo beans", "chickpea"),
            Map.entry("chickpeas", "chickpea")
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

        int commaIndex = cleaned.indexOf(',');
        if (commaIndex >= 0) {
            cleaned = cleaned.substring(0, commaIndex);
        }

        cleaned = cleaned.replaceAll("[^a-z0-9\\s]", " ");
        cleaned = collapseWhitespace(cleaned);
        cleaned = applyAlias(cleaned);
        cleaned = singularizePhrase(cleaned);
        cleaned = collapseWhitespace(cleaned);
        return applyAlias(cleaned);
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
