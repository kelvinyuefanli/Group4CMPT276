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

        prompt.append("If a quantity is unknown, return null. ");
        prompt.append("Do not combine quantity and unit into one field, but always include the ingredient name. ");
        prompt.append("Return ONLY valid JSON (no markdown fences, no extra text) matching this schema:\n");
        prompt.append(RECIPE_JSON_SCHEMA);

        String raw = generateContent(prompt.toString());
        if (raw == null) return null;
        return parseJson(raw, GeminiRecipeDto.class);
    }

    public GeminiMealPlanDto generateMealPlan(String pantryIngredients, int servingSize, String preferences) {
        return generateMealPlan(pantryIngredients, servingSize, preferences, null, false, null, null, null);
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

    public GeminiMealPlanDto generateMealPlan(String pantryIngredients, int servingSize, String dietaryRestrictions,
            String allergies, boolean rotateCuisines, String preferredCuisines,
            String dislikedFoods, String mealSchedule) {

        Set<MealPlanGenerationSupport.MealSlot> requestedSlots =
            MealPlanGenerationSupport.expectedSlots(mealSchedule);
        LinkedHashMap<MealPlanGenerationSupport.MealSlot, GeminiMealPlanDto.MealEntry> acceptedMeals =
            new LinkedHashMap<>();
        List<MealPlanGenerationSupport.MealSlot> remainingSlots = new ArrayList<>(
            requestedSlots
        );
        String lastRaw = null;

        for (int attempt = 1; attempt <= 3 && !remainingSlots.isEmpty(); attempt++) {
            Set<MealPlanGenerationSupport.MealSlot> remainingSlotSet = new LinkedHashSet<>(remainingSlots);
            String prompt = buildMealPlanPrompt(
                pantryIngredients,
                servingSize,
                dietaryRestrictions,
                allergies,
                rotateCuisines,
                preferredCuisines,
                dislikedFoods,
                MealPlanGenerationSupport.serializeSlots(remainingSlotSet),
                remainingSlotSet,
                attempt > 1,
                !acceptedMeals.isEmpty()
            );

            lastRaw = generateContent(prompt);
            GeminiMealPlanDto dto = parseJson(lastRaw, GeminiMealPlanDto.class);
            MealPlanGenerationSupport.ExtractedMeals extracted =
                MealPlanGenerationSupport.extractValidMeals(dto, remainingSlotSet, servingSize);

            acceptedMeals.putAll(extracted.acceptedMeals());
            remainingSlots = extracted.missingSlots();
        }

        if (!remainingSlots.isEmpty()) {
            log.warn(
                "Gemini meal plan generation incomplete. Missing {} slots: {}",
                remainingSlots.size(),
                MealPlanGenerationSupport.describeSlots(new LinkedHashSet<>(remainingSlots))
            );
            if (lastRaw != null) {
                log.debug("Last incomplete Gemini meal plan response: {}", lastRaw);
            }
            return null;
        }

        return MealPlanGenerationSupport.buildMealPlan(acceptedMeals);
    }

    public GeminiMealPlanDto generateMealPlan(String pantryIngredients, String dietaryRestrictions,
            String allergies, boolean rotateCuisines, String preferredCuisines,
            String dislikedFoods, String mealSchedule) {
        return generateMealPlan(pantryIngredients, dietaryRestrictions, allergies,
                rotateCuisines, preferredCuisines, dislikedFoods, mealSchedule,
                null, null, null, 2);
    }

    /**
     * Randomly samples up to {@code max} items from a comma-separated list.
     * Used to auto-rotate proteins/vegetables/fruits each week so the user
     * gets variety without re-selecting manually.
     * Package-private for testing.
     */
    static String sampleFromList(String commaSeparated, int max) {
        if (commaSeparated == null || commaSeparated.isBlank()) return commaSeparated;
        List<String> items = new ArrayList<>();
        for (String item : commaSeparated.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) items.add(trimmed);
        }
        if (items.size() <= max) return commaSeparated;
        Collections.shuffle(items);
        return String.join(", ", items.subList(0, max));
    }

    public GeminiMealPlanDto generateMealPlan(String pantryIngredients, String dietaryRestrictions,
            String allergies, boolean rotateCuisines, String preferredCuisines,
            String dislikedFoods, String mealSchedule,
            String preferredProteins, String preferredVegetables, String preferredFruits,
            int servingSize) {

        // Auto-rotate: if user selected more than the target count, sample a subset
        preferredProteins = sampleFromList(preferredProteins, 3);
        preferredVegetables = sampleFromList(preferredVegetables, 5);
        preferredFruits = sampleFromList(preferredFruits, 3);

        Set<MealPlanGenerationSupport.MealSlot> requestedSlots =
            MealPlanGenerationSupport.expectedSlots(mealSchedule);
        LinkedHashMap<MealPlanGenerationSupport.MealSlot, GeminiMealPlanDto.MealEntry> acceptedMeals =
            new LinkedHashMap<>();
        List<MealPlanGenerationSupport.MealSlot> remainingSlots = new ArrayList<>(requestedSlots);
        String lastRaw = null;

        for (int attempt = 1; attempt <= 3 && !remainingSlots.isEmpty(); attempt++) {
            Set<MealPlanGenerationSupport.MealSlot> remainingSlotSet = new LinkedHashSet<>(remainingSlots);
            String prompt = buildFullMealPlanPrompt(
                pantryIngredients, servingSize, dietaryRestrictions, allergies,
                rotateCuisines, preferredCuisines, dislikedFoods,
                MealPlanGenerationSupport.serializeSlots(remainingSlotSet),
                remainingSlotSet,
                preferredProteins, preferredVegetables, preferredFruits,
                attempt > 1, !acceptedMeals.isEmpty()
            );

            lastRaw = generateContent(prompt);
            GeminiMealPlanDto dto = parseJson(lastRaw, GeminiMealPlanDto.class);
            MealPlanGenerationSupport.ExtractedMeals extracted =
                MealPlanGenerationSupport.extractValidMeals(dto, remainingSlotSet, servingSize);

            acceptedMeals.putAll(extracted.acceptedMeals());
            remainingSlots = extracted.missingSlots();
        }

        if (!remainingSlots.isEmpty()) {
            log.warn(
                "Gemini meal plan generation incomplete. Missing {} slots: {}",
                remainingSlots.size(),
                MealPlanGenerationSupport.describeSlots(new LinkedHashSet<>(remainingSlots))
            );
            if (lastRaw != null) {
                log.debug("Last incomplete Gemini meal plan response: {}", lastRaw);
            }
            return null;
        }

        return MealPlanGenerationSupport.buildMealPlan(acceptedMeals);
    }

    private String buildMealPlanPrompt(String pantryIngredients, int servingSize, String dietaryRestrictions,
            String allergies, boolean rotateCuisines, String preferredCuisines,
            String dislikedFoods, String mealSchedule, Set<MealPlanGenerationSupport.MealSlot> requestedSlots,
            boolean isRetry, boolean partialPlanAlreadyExists) {

        StringBuilder prompt = new StringBuilder();
        if (mealSchedule != null && !mealSchedule.isBlank()) {
            prompt.append("Generate meals ONLY for these day and meal slots: ")
                .append(MealPlanGenerationSupport.describeSlots(requestedSlots))
                .append(". ");
        } else {
            prompt.append("Create a weekly meal plan (Monday to Sunday, 3 meals per day: BREAKFAST, LUNCH, DINNER). ");
        }

        prompt.append("Return exactly ").append(requestedSlots.size()).append(" meal entries. ");
        prompt.append("Each recipe must make ").append(servingSize).append(" servings. ");
        prompt.append("Every dayOfWeek must be one of MONDAY to SUNDAY and every mealType must be BREAKFAST, LUNCH, or DINNER. ");

        if (pantryIngredients != null && !pantryIngredients.isBlank()) {
            prompt.append("Use primarily these available ingredients: ").append(pantryIngredients).append(". ");
        } else {
            prompt.append("If pantry ingredients are missing, use common accessible grocery ingredients. ");
        }

        if (dietaryRestrictions != null && !dietaryRestrictions.isBlank()) {
            prompt.append("Dietary restrictions (MUST follow): ").append(sanitizeInput(dietaryRestrictions)).append(". ");
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

        prompt.append("Base meals on the provided ingredients as much as possible and minimize extra ingredients. ");
        prompt.append("For each recipe include title, cuisine, cookTimeMinutes, servings, full instructions, ");
        prompt.append("and an ingredients list with name, quantity, and unit. ");
        prompt.append("If a quantity is unknown, use null instead of free-form text whenever possible. ");
        prompt.append("Keep scalar fields as plain strings or numbers, not arrays or nested wrappers. ");
        if (isRetry) {
            prompt.append("Previous output was incomplete or invalid. Double-check every requested slot before you answer. ");
        }
        if (partialPlanAlreadyExists) {
            prompt.append("Only fill the missing slots requested in this prompt and do not repeat meals for other slots. ");
        }
        prompt.append("Return ONLY valid JSON (no markdown fences, no extra text) matching this schema:\n");
        prompt.append(MEAL_PLAN_JSON_SCHEMA);
        return prompt.toString();
    }

    private String buildFullMealPlanPrompt(String pantryIngredients, int servingSize, String dietaryRestrictions,
            String allergies, boolean rotateCuisines, String preferredCuisines,
            String dislikedFoods, String mealSchedule, Set<MealPlanGenerationSupport.MealSlot> requestedSlots,
            String preferredProteins, String preferredVegetables, String preferredFruits,
            boolean isRetry, boolean partialPlanAlreadyExists) {

        StringBuilder prompt = new StringBuilder();
        if (mealSchedule != null && !mealSchedule.isBlank()) {
            prompt.append("Generate meals ONLY for these day and meal slots: ")
                .append(MealPlanGenerationSupport.describeSlots(requestedSlots))
                .append(". ");
        } else {
            prompt.append("Create a weekly meal plan (Monday to Sunday, 3 meals per day: BREAKFAST, LUNCH, DINNER). ");
        }

        prompt.append("Return exactly ").append(requestedSlots.size()).append(" meal entries. ");
        prompt.append("Cooking for ").append(servingSize).append(servingSize == 1 ? " person" : " people").append(". ");

        // ---- NUTRITIONAL BALANCE GUIDELINES ----
        prompt.append("\n\nNUTRITIONAL BALANCE RULES (MUST FOLLOW):\n");
        prompt.append("Every meal MUST include protein, vegetables or fruit, and carbohydrates. ");
        prompt.append("Follow the plate model: 1/2 plate vegetables+fruit, 1/4 plate protein, 1/4 plate carbs.\n");
        prompt.append("- BREAKFAST: 50-60% carbs, 20-25% protein, 20-30% vegetables/fruit. Include eggs, yogurt, or meat for protein.\n");
        prompt.append("- LUNCH: 40-50% carbs, 25-30% protein, 25-35% vegetables. Most balanced meal of the day.\n");
        prompt.append("- DINNER: 30-40% carbs, 25-35% protein, 30-40% vegetables. Emphasize vegetables, smaller carb portions.\n");
        prompt.append("NEVER create a meal that is only carbs and condiments (e.g., no 'ketchup sandwich' or 'buttered toast' as a full meal). ");
        prompt.append("Every meal must have a real protein source and real vegetables or fruit.\n\n");

        // ---- WEEKLY SHOPPING PROTEINS ----
        if (preferredProteins != null && !preferredProteins.isBlank()) {
            prompt.append("WEEKLY PROTEINS TO BUY: The user will buy these proteins for the week: ")
                  .append(sanitizeInput(preferredProteins)).append(". ");
            prompt.append("Use ONLY these 2-3 protein types across the entire week. ");
            prompt.append("Plan portions so one purchase lasts multiple meals (e.g., 1 pack of chicken thighs for 3 dinners, ");
            prompt.append("1 dozen eggs for 4 breakfasts). Reuse the same protein across different recipes for efficiency.\n");
        } else {
            prompt.append("Include a variety of proteins across the week (chicken, beef, fish, eggs, beans, tofu). ");
            prompt.append("Limit to 2-3 protein types total to minimize grocery shopping.\n");
        }

        // ---- WEEKLY SHOPPING VEGETABLES & FRUITS ----
        if (preferredVegetables != null && !preferredVegetables.isBlank()) {
            prompt.append("WEEKLY VEGETABLES TO BUY: ").append(sanitizeInput(preferredVegetables)).append(". ");
            prompt.append("Use these vegetables across multiple meals throughout the week. ");
            prompt.append("Plan so one bunch/pack is used in 3-4 meals (e.g., 1 head of broccoli across 3 dinners).\n");
        } else {
            prompt.append("Include vegetables in every lunch and dinner. Use 4-5 vegetable types across the week, reusing them across meals.\n");
        }

        if (preferredFruits != null && !preferredFruits.isBlank()) {
            prompt.append("WEEKLY FRUITS TO BUY: ").append(sanitizeInput(preferredFruits)).append(". ");
            prompt.append("Include fruit in breakfasts and as sides. Plan portions for the week.\n");
        }

        // ---- PANTRY STAPLES ----
        if (pantryIngredients != null && !pantryIngredients.isBlank()) {
            prompt.append("\nPANTRY STAPLES (already on hand, do not add to grocery list): ")
                  .append(sanitizeInput(pantryIngredients)).append(". ");
            prompt.append("Use these freely as base ingredients but do NOT make meals from ONLY pantry staples. ");
            prompt.append("Pantry staples are supporting ingredients, not the main dish.\n");
        }

        // ---- DIETARY CONSTRAINTS ----
        if (dietaryRestrictions != null && !dietaryRestrictions.isBlank()) {
            prompt.append("\nDietary restrictions (MUST follow): ")
                  .append(sanitizeInput(dietaryRestrictions)).append(". ");
        }

        if (allergies != null && !allergies.isBlank()) {
            prompt.append("\nCRITICAL: This person has SEVERE food allergies to: ")
                  .append(sanitizeInput(allergies))
                  .append(". ABSOLUTELY DO NOT include any of these allergens or their derivatives in ANY recipe. ");
        }

        if (preferredCuisines != null && !preferredCuisines.isBlank()) {
            prompt.append("\nPreferred cuisines: ").append(sanitizeInput(preferredCuisines)).append(". ");
            if (rotateCuisines) {
                prompt.append("Rotate between these cuisines across different days so the week has variety. ");
            }
            prompt.append("When cuisine preferences conflict with dietary restrictions, prioritize dietary safety and choose the closest compliant cuisine option. ");
        }

        if (dislikedFoods != null && !dislikedFoods.isBlank()) {
            prompt.append("\nAvoid these disliked foods: ").append(sanitizeInput(dislikedFoods)).append(". ");
        }

        prompt.append("\nFor each recipe include title, cuisine, cookTimeMinutes, servings, full instructions, ");
        prompt.append("and a COMPLETE ingredients list with name, quantity, and unit (include ALL ingredients including vegetables, proteins, grains). ");
        prompt.append("If a quantity is unknown, use null instead of free-form text whenever possible. ");
        prompt.append("Keep scalar fields as plain strings or numbers, not arrays or nested wrappers. ");
        if (isRetry) {
            prompt.append("Previous output was incomplete or invalid. Double-check every requested slot before you answer. ");
        }
        if (partialPlanAlreadyExists) {
            prompt.append("Only fill the missing slots requested in this prompt and do not repeat meals for other slots. ");
        }
        prompt.append("Return ONLY valid JSON (no markdown fences, no extra text) matching this schema:\n");
        prompt.append(MEAL_PLAN_JSON_SCHEMA);
        return prompt.toString();
    }

    <T> T parseJson(String raw, Class<T> type) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = extractJsonPayload(raw);
        try {
            return objectMapper.readValue(cleaned, type);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Gemini JSON response: {}", e.getMessage());
            log.debug("Raw response was: {}", raw);
            return null;
        }
    }

    private String extractJsonPayload(String raw) {
        String cleaned = raw.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "");
            cleaned = cleaned.replaceFirst("```\\s*$", "");
            cleaned = cleaned.strip();
        }

        int objectStart = cleaned.indexOf('{');
        if (objectStart < 0) {
            return cleaned;
        }

        int objectEnd = findJsonObjectEnd(cleaned, objectStart);
        if (objectEnd < 0) {
            return cleaned;
        }

        return cleaned.substring(objectStart, objectEnd + 1);
    }

    private int findJsonObjectEnd(String text, int startIndex) {
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = startIndex; i < text.length(); i++) {
            char current = text.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }

            if (current == '\\' && inString) {
                escaping = true;
                continue;
            }

            if (current == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    // ---- Error type tracking ----

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

    // ---- Low-level Gemini API interaction ----

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
