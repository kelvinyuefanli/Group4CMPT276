package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.mealplanning.dto.GeminiMealPlanDto;
import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final List<String> PREFERRED_MODELS = List.of(
        "models/gemini-2.0-flash",
        "models/gemini-1.5-flash-latest",
        "models/gemini-1.5-flash",
        "models/gemini-1.5-pro",
        "models/gemini-pro"
    );

    private static final int MAX_USER_INPUT_LENGTH = 500;

    private static final String RECIPE_JSON_SCHEMA = """
        {
          "title": "string",
          "cuisine": "string or null",
          "cookTimeMinutes": integer,
          "servings": integer,
          "instructions": "string with numbered steps",
          "ingredients": [
            { "name": "string", "quantity": number_or_null, "unit": "string or null" }
          ]
        }""";

    private static final String MEAL_PLAN_JSON_SCHEMA = """
        {
          "meals": [
            {
              "dayOfWeek": "MONDAY|TUESDAY|...|SUNDAY",
              "mealType": "BREAKFAST|LUNCH|DINNER",
              "recipe": { ...same recipe schema... }
            }
          ]
        }""";

    @Value("${gemini.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sanitises user-supplied text before including it in a Gemini prompt.
     * Strips control characters, limits length, and removes prompt injection attempts.
     */
    static String sanitizeInput(String input) {
        if (input == null) return null;
        // Strip control characters (keep standard whitespace)
        String cleaned = input.replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", "");
        // Limit length
        if (cleaned.length() > MAX_USER_INPUT_LENGTH) {
            cleaned = cleaned.substring(0, MAX_USER_INPUT_LENGTH);
        }
        return cleaned.strip();
    }

    public GeminiRecipeDto generateRecipe(String ingredients, String cuisine) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a detailed recipe using these ingredients: ")
              .append(sanitizeInput(ingredients)).append(". ");

        if (cuisine != null && !cuisine.isBlank()) {
            prompt.append("The recipe should be ").append(sanitizeInput(cuisine)).append(" cuisine. ");
        }

        prompt.append("Return ONLY valid JSON (no markdown fences, no extra text) matching this schema:\n");
        prompt.append(RECIPE_JSON_SCHEMA);

        String raw = generateContent(prompt.toString());
        if (raw == null) return null;
        return parseJson(raw, GeminiRecipeDto.class);
    }

    public GeminiMealPlanDto generateMealPlan(String pantryIngredients, String preferences) {
        return generateMealPlan(pantryIngredients, preferences, null, false, null, null, null);
    }

    /**
     * Generates a single recipe for a specific meal slot, used for allergy re-generation.
     */
    public GeminiRecipeDto generateSingleMeal(String dayOfWeek, String mealType,
            String allergies, String dietaryRestrictions, String preferredCuisines) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a single recipe for ").append(dayOfWeek).append(" ").append(mealType).append(". ");

        if (allergies != null && !allergies.isBlank()) {
            prompt.append("CRITICAL: This person has SEVERE food allergies to: ")
                  .append(sanitizeInput(allergies))
                  .append(". ABSOLUTELY DO NOT include any of these allergens or their derivatives in the recipe. ");
        }

        if (dietaryRestrictions != null && !dietaryRestrictions.isBlank()) {
            prompt.append("Dietary restrictions (MUST follow): ").append(sanitizeInput(dietaryRestrictions)).append(". ");
        }

        if (preferredCuisines != null && !preferredCuisines.isBlank()) {
            prompt.append("Preferred cuisines: ").append(sanitizeInput(preferredCuisines)).append(". ");
        }

        prompt.append("Return ONLY valid JSON (no markdown fences, no extra text) matching this schema:\n");
        prompt.append(RECIPE_JSON_SCHEMA);

        String raw = generateContent(prompt.toString());
        if (raw == null) return null;
        return parseJson(raw, GeminiRecipeDto.class);
    }

    public GeminiMealPlanDto generateMealPlan(String pantryIngredients, String dietaryRestrictions,
            String allergies, boolean rotateCuisines, String preferredCuisines,
            String dislikedFoods, String mealSchedule) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a weekly meal plan (Monday to Sunday, 3 meals per day: BREAKFAST, LUNCH, DINNER). ");

        if (pantryIngredients != null && !pantryIngredients.isBlank()) {
            prompt.append("Use primarily these available ingredients: ")
                  .append(sanitizeInput(pantryIngredients)).append(". ");
        }

        if (dietaryRestrictions != null && !dietaryRestrictions.isBlank()) {
            prompt.append("Dietary restrictions (MUST follow): ")
                  .append(sanitizeInput(dietaryRestrictions)).append(". ");
        }

        if (allergies != null && !allergies.isBlank()) {
            prompt.append("CRITICAL: This person has SEVERE food allergies to: ")
                  .append(sanitizeInput(allergies))
                  .append(". ABSOLUTELY DO NOT include any of these allergens or their derivatives in ANY recipe. ");
        }

        if (preferredCuisines != null && !preferredCuisines.isBlank()) {
            prompt.append("Preferred cuisines: ").append(sanitizeInput(preferredCuisines)).append(". ");
            if (rotateCuisines) {
                prompt.append("Rotate between these cuisines across different days so the week has variety. ");
            }
        }

        if (dislikedFoods != null && !dislikedFoods.isBlank()) {
            prompt.append("Avoid these disliked foods: ").append(sanitizeInput(dislikedFoods)).append(". ");
        }

        if (mealSchedule != null && !mealSchedule.isBlank()) {
            prompt.append("Only generate meals for these day/meal slots (skip the rest): ")
                  .append(sanitizeInput(mealSchedule)).append(". ");
        }

        prompt.append("Base meals on the provided ingredients as much as possible and minimize extra ingredients. ");
        prompt.append("For each recipe include title, cuisine, cookTimeMinutes, servings, full instructions, ");
        prompt.append("and an ingredients list with name, quantity, and unit. ");
        prompt.append("Return ONLY valid JSON (no markdown fences, no extra text) matching this schema:\n");
        prompt.append(MEAL_PLAN_JSON_SCHEMA);

        String raw = generateContent(prompt.toString());
        if (raw == null) return null;
        return parseJson(raw, GeminiMealPlanDto.class);
    }

    <T> T parseJson(String raw, Class<T> type) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "");
            cleaned = cleaned.replaceFirst("```\\s*$", "");
            cleaned = cleaned.strip();
        }
        try {
            return objectMapper.readValue(cleaned, type);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Gemini JSON response: {}", e.getMessage());
            log.debug("Raw response was: {}", raw);
            return null;
        }
    }

    // ---- Low-level Gemini API interaction ----

    /**
     * Encapsulates the result of a Gemini API call, including error context
     * for better user-facing error messages.
     */
    public enum GeminiErrorType {
        NONE,
        AUTH_FAILURE,
        TIMEOUT,
        MALFORMED_RESPONSE,
        NO_MODELS_AVAILABLE,
        UNKNOWN
    }

    private GeminiErrorType lastErrorType = GeminiErrorType.NONE;

    /** Returns the error type from the most recent generateContent call. */
    public GeminiErrorType getLastErrorType() {
        return lastErrorType;
    }

    /** Returns a user-friendly error message for the most recent failure. */
    public String getLastErrorMessage() {
        return switch (lastErrorType) {
            case AUTH_FAILURE -> "Invalid API key. Please check your Gemini API key configuration.";
            case TIMEOUT -> "The AI service is temporarily unavailable. Please try again in a moment.";
            case MALFORMED_RESPONSE -> "The AI returned an unexpected response. Please try again.";
            case NO_MODELS_AVAILABLE -> "No AI models are currently available. Please try again later.";
            case UNKNOWN -> "An unexpected error occurred while generating your meal plan. Please try again.";
            case NONE -> null;
        };
    }

    private String generateContent(String prompt) {
        lastErrorType = GeminiErrorType.NONE;

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(part));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            List<String> modelNames = resolveModelNames();
            RestClientResponseException lastApiException = null;

            for (String modelName : modelNames) {
                try {
                    @SuppressWarnings("rawtypes")
                    ResponseEntity<Map> response = restTemplate.exchange(
                        BASE_URL + "/" + modelName + ":generateContent?key=" + apiKey,
                        HttpMethod.POST,
                        entity,
                        Map.class
                    );

                    String generatedText = extractGeneratedText(response.getBody());
                    if (generatedText != null && !generatedText.isBlank()) {
                        return generatedText;
                    }
                } catch (RestClientResponseException apiException) {
                    lastApiException = apiException;
                    int status = apiException.getStatusCode().value();
                    if (status == 401 || status == 403) {
                        lastErrorType = GeminiErrorType.AUTH_FAILURE;
                        break;
                    }
                }
            }

            if (lastApiException != null) {
                log.error("Gemini API error ({}): {}",
                    lastApiException.getStatusCode().value(),
                    lastApiException.getResponseBodyAsString());
                if (lastErrorType == GeminiErrorType.NONE) {
                    lastErrorType = GeminiErrorType.UNKNOWN;
                }
            } else {
                // All models returned empty/null responses
                lastErrorType = GeminiErrorType.MALFORMED_RESPONSE;
            }
            return null;
        } catch (ResourceAccessException e) {
            log.error("Gemini API timeout/network error: {}", e.getMessage());
            lastErrorType = GeminiErrorType.TIMEOUT;
            return null;
        } catch (IllegalStateException e) {
            log.error("Gemini model resolution failed: {}", e.getMessage());
            lastErrorType = GeminiErrorType.NO_MODELS_AVAILABLE;
            return null;
        } catch (Exception e) {
            log.error("Unexpected error calling Gemini API", e);
            lastErrorType = GeminiErrorType.UNKNOWN;
            return null;
        }
    }

    private List<String> resolveModelNames() {
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.exchange(
            BASE_URL + "/models?key=" + apiKey,
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body == null || !(body.get("models") instanceof List<?> modelsRaw)) {
            throw new IllegalStateException("Unable to fetch available Gemini models.");
        }

        List<Map<String, Object>> supportedModels = new ArrayList<>();
        for (Object modelObj : modelsRaw) {
            if (!(modelObj instanceof Map<?, ?> modelMapRaw)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> modelMap = (Map<String, Object>) modelMapRaw;
            Object methodsObj = modelMap.get("supportedGenerationMethods");
            if (!(methodsObj instanceof List<?> methods)) {
                continue;
            }
            if (methods.contains("generateContent")) {
                supportedModels.add(modelMap);
            }
        }

        List<String> orderedNames = new ArrayList<>();
        for (String preferred : PREFERRED_MODELS) {
            for (Map<String, Object> model : supportedModels) {
                if (preferred.equals(model.get("name"))) {
                    orderedNames.add(preferred);
                }
            }
        }

        for (Map<String, Object> model : supportedModels) {
            String modelName = String.valueOf(model.get("name"));
            if (!orderedNames.contains(modelName)) {
                orderedNames.add(modelName);
            }
        }

        if (!orderedNames.isEmpty()) {
            return orderedNames;
        }

        if (!supportedModels.isEmpty()) {
            return List.of(String.valueOf(supportedModels.get(0).get("name")));
        }

        throw new IllegalStateException("No Gemini models with generateContent support are available for this API key.");
    }

    @SuppressWarnings("unchecked")
    private String extractGeneratedText(Map<String, Object> body) {
        if (body == null || !(body.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }

        Object firstCandidateObj = candidates.get(0);
        if (!(firstCandidateObj instanceof Map<?, ?> firstCandidateRaw)) {
            return null;
        }

        Object contentObj = firstCandidateRaw.get("content");
        if (!(contentObj instanceof Map<?, ?> contentRaw)) {
            return null;
        }

        Object partsObj = contentRaw.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return null;
        }

        Object firstPartObj = parts.get(0);
        if (!(firstPartObj instanceof Map<?, ?> firstPartRaw)) {
            return null;
        }

        Object textObj = firstPartRaw.get("text");
        return textObj == null ? null : String.valueOf(textObj);
    }
}
