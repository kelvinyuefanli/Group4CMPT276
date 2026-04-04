package com._6.group4.smartcart.mealplanning.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiRecipeDto(
    String title,
    String cuisine,
    Integer cookTimeMinutes,
    Integer servings,
    @JsonDeserialize(using = FlexibleStringDeserializer.class)
    String instructions,
    List<IngredientDto> ingredients
) {
    private static final Pattern INTEGER_TOKEN_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    public GeminiRecipeDto {
        ingredients = ingredients == null ? List.of() : ingredients;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static GeminiRecipeDto from(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        return new GeminiRecipeDto(
            readInlineText(firstPresent(node, "title", "name")),
            readInlineText(firstPresent(node, "cuisine", "cuisineType")),
            readIntegerValue(firstPresent(node, "cookTimeMinutes", "cookTime", "cook_time_minutes", "cook_time")),
            readIntegerValue(firstPresent(node, "servings", "servingSize", "yield")),
            readMultilineText(firstPresent(node, "instructions", "steps", "method", "directions")),
            readIngredients(firstPresent(node, "ingredients", "ingredientList", "items"))
        );
    }

    public List<IngredientDto> normalizedIngredients() {
        List<IngredientDto> normalized = new ArrayList<>();
        for (IngredientDto ingredient : ingredients) {
            if (ingredient != null && ingredient.hasName()) {
                normalized.add(ingredient);
            }
        }
        return normalized;
    }

    /**
     * Handles Gemini returning instructions as either a string or an array of strings.
     * Arrays are joined with newlines.
     */
    static class FlexibleStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken().isStructStart()) {
                // It's an array — read all elements and join
                List<?> items = p.readValueAs(List.class);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append(items.get(i));
                }
                return sb.toString();
            }
            return p.getValueAsString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IngredientDto(
        String name,
        Object quantity,
        String unit
    ) {
        private static final Pattern QUANTITY_TOKEN_PATTERN =
            Pattern.compile("(?:(?:\\d+\\s+\\d+/\\d+)|(?:\\d+/\\d+)|(?:\\d+(?:\\.\\d+)?))");

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static IngredientDto from(JsonNode node) {
            if (node == null || node.isNull()) {
                return null;
            }

            if (node.isTextual()) {
                String ingredientText = readInlineText(node);
                return ingredientText == null ? null : new IngredientDto(ingredientText, null, null);
            }

            if (node.isObject()) {
                String parsedName = readInlineText(firstPresent(node, "name", "ingredient", "item"));
                Object parsedQuantity = quantityValue(node.get("quantity"));
                String parsedUnit = readInlineText(firstPresent(node, "unit", "measurementUnit", "measure"));

                if (parsedName == null && parsedQuantity == null && parsedUnit == null) {
                    return null;
                }

                return new IngredientDto(parsedName != null ? parsedName : "unknown", parsedQuantity, parsedUnit);
            }

            String fallbackText = cleanText(node.asText());
            return fallbackText == null ? null : new IngredientDto(fallbackText, null, null);
        }

        public boolean hasName() {
            return name != null && !name.isBlank();
        }

        public String safeName() {
            return hasName() ? name.trim() : "unknown";
        }

        public String safeUnit() {
            return cleanText(unit);
        }

        public Double quantityAsDouble() {
            return parseQuantity(quantity);
        }

        private static Object quantityValue(JsonNode node) {
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isNumber()) {
                return node.numberValue();
            }
            String rawValue = readCompactText(node);
            if (rawValue == null) {
                return null;
            }

            String normalized = rawValue.toLowerCase(Locale.ROOT);
            if ("unknown".equals(normalized) || "n/a".equals(normalized) || "none".equals(normalized)) {
                return null;
            }
            return rawValue;
        }

        private static Double parseQuantity(Object rawQuantity) {
            if (rawQuantity == null) {
                return null;
            }
            if (rawQuantity instanceof Number number) {
                return number.doubleValue();
            }

            String value = normalizeFractions(cleanText(rawQuantity));
            if (value == null) {
                return null;
            }

            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
                // Fall through to mixed-number or embedded-token parsing.
            }

            Matcher matcher = QUANTITY_TOKEN_PATTERN.matcher(value);
            if (!matcher.find()) {
                return null;
            }

            return parseNumberToken(matcher.group());
        }

        private static Double parseNumberToken(String token) {
            if (token == null || token.isBlank()) {
                return null;
            }

            String cleanedToken = token.trim();
            if (cleanedToken.contains(" ")) {
                String[] mixedParts = cleanedToken.split("\\s+", 2);
                if (mixedParts.length == 2) {
                    Double whole = parseNumberToken(mixedParts[0]);
                    Double fraction = parseNumberToken(mixedParts[1]);
                    if (whole != null && fraction != null) {
                        return whole + fraction;
                    }
                }
            }

            if (cleanedToken.contains("/")) {
                String[] fractionParts = cleanedToken.split("/", 2);
                if (fractionParts.length == 2) {
                    try {
                        double numerator = Double.parseDouble(fractionParts[0].trim());
                        double denominator = Double.parseDouble(fractionParts[1].trim());
                        if (denominator != 0) {
                            return numerator / denominator;
                        }
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }

            try {
                return Double.parseDouble(cleanedToken);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static String normalizeFractions(String value) {
            if (value == null) {
                return null;
            }

            String normalized = value
                .replace('\u00A0', ' ')
                .replace("\u00BD", " 1/2 ")
                .replace("\u00BC", " 1/4 ")
                .replace("\u00BE", " 3/4 ")
                .replace("\u2153", " 1/3 ")
                .replace("\u2154", " 2/3 ")
                .replace("\u215B", " 1/8 ")
                .replace("\u215C", " 3/8 ")
                .replace("\u215D", " 5/8 ")
                .replace("\u215E", " 7/8 ")
                .replaceAll("\\s+", " ")
                .trim();

            return normalized.isBlank() ? null : normalized;
        }

        private static String cleanText(Object rawValue) {
            if (rawValue == null) {
                return null;
            }

            String text = String.valueOf(rawValue).trim();
            if (text.isBlank() || "null".equalsIgnoreCase(text)) {
                return null;
            }

            return text;
        }
    }

    static JsonNode firstPresent(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            JsonNode candidate = node.get(fieldName);
            if (candidate != null && !candidate.isNull()) {
                return candidate;
            }
        }
        return null;
    }

    static Integer readIntegerValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.intValue();
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                Integer value = readIntegerValue(child);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }
        if (node.isObject()) {
            Integer directValue = readIntegerValue(firstPresent(node, "value", "amount", "minutes", "count"));
            if (directValue != null) {
                return directValue;
            }
        }

        String text = readCompactText(node);
        if (text == null) {
            return null;
        }

        Matcher matcher = INTEGER_TOKEN_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        try {
            return (int) Math.round(Double.parseDouble(matcher.group()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static String readInlineText(JsonNode node) {
        return joinText(node, ", ");
    }

    static String readCompactText(JsonNode node) {
        return joinText(node, " ");
    }

    static String readMultilineText(JsonNode node) {
        return joinText(node, "\n");
    }

    private static String joinText(JsonNode node, String separator) {
        List<String> parts = new ArrayList<>();
        collectText(node, parts, separator);
        return parts.isEmpty() ? null : String.join(separator, parts);
    }

    private static void collectText(JsonNode node, List<String> parts, String separator) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            String text = IngredientDto.cleanText(node.asText());
            if (text != null) {
                parts.add(text);
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectText(child, parts, separator);
            }
            return;
        }

        if (!node.isObject()) {
            String text = IngredientDto.cleanText(node.asText());
            if (text != null) {
                parts.add(text);
            }
            return;
        }

        boolean matchedPreferredField = false;
        for (String key : List.of("text", "value", "name", "title", "description", "instruction", "content", "label")) {
            JsonNode child = node.get(key);
            if (child != null && !child.isNull()) {
                collectText(child, parts, separator);
                matchedPreferredField = true;
            }
        }

        if (matchedPreferredField) {
            return;
        }

        for (JsonNode child : node) {
            collectText(child, parts, separator);
        }
    }

    private static List<IngredientDto> readIngredients(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }

        List<IngredientDto> parsed = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode child : node) {
                IngredientDto ingredient = IngredientDto.from(child);
                if (ingredient != null) {
                    parsed.add(ingredient);
                }
            }
            return parsed;
        }

        IngredientDto ingredient = IngredientDto.from(node);
        return ingredient == null ? List.of() : List.of(ingredient);
    }
}
