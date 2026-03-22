package com._6.group4.smartcart.mealplanning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiRecipeDto(
    String title,
    String cuisine,
    Integer cookTimeMinutes,
    Integer servings,
    @JsonDeserialize(using = FlexibleStringDeserializer.class)
    String instructions,
    List<?> ingredients  // Accept both objects and strings
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IngredientDto(
        String name,
        Object quantity,  // Accept Double, String, or any fraction format
        String unit
    ) {}

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
}
