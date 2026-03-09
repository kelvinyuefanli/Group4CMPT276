package com.smartcart.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final List<String> PREFERRED_MODELS = List.of(
        "models/gemini-1.5-flash-latest",
        "models/gemini-1.5-flash",
        "models/gemini-1.5-pro",
        "models/gemini-2.0-flash",
        "models/gemini-pro"
    );

    public String generateMealPlan(String pantryIngredients, String preferences) {
        String prompt = "Create a weekly meal plan (Monday to Sunday) using primarily these available ingredients: "
                        + pantryIngredients + ". "
                        + "Dietary preferences/constraints: " + preferences + ". "
                        + "Requirements: "
                        + "1) First output a clean HTML table with columns Day, Breakfast, Lunch, Dinner. "
                        + "2) Base meals on the provided ingredients as much as possible and minimize extra ingredients. "
                        + "3) After the table, output detailed recipes for every meal in the weekly plan. "
                        + "4) For each recipe include: recipe name (<h3>), exact ingredient measurements (<ul> with units like cups, grams, tbsp), numbered instructions (<ol>), cook time, and servings. "
                        + "5) Use only plain HTML tags. No markdown code blocks and no CSS classes.";
        
        return generateContent(prompt);
    }

    public String generateRecipe(String ingredients, String cuisine) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a detailed recipe using the following ingredients: ").append(ingredients).append(". ");
        
        if (cuisine != null && !cuisine.isBlank()) {
            prompt.append("The recipe should be ").append(cuisine).append(" cuisine. ");
        }
        
        prompt.append("Format the output as HTML with the following sections: ");
        prompt.append("1. Recipe title in an <h3> tag ");
        prompt.append("2. Ingredients list in a <ul> with exact measurements and units ");
        prompt.append("3. Step-by-step instructions in an <ol> ");
        prompt.append("4. Cooking time and servings information ");
        prompt.append("Do not include markdown code blocks or CSS classes. Use plain HTML tags only.");
        
        return generateContent(prompt.toString());
    }

    private String generateContent(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        
        // JSON Payload Construction
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
            List<String> modelNames = resolveModelNames(restTemplate);
            RestClientResponseException lastApiException = null;

            for (String modelName : modelNames) {
                try {
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
                    if (apiException.getRawStatusCode() == 401 || apiException.getRawStatusCode() == 403) {
                        break;
                    }
                }
            }

            if (lastApiException != null) {
                return "<p>Gemini API error (" + lastApiException.getRawStatusCode() + "): "
                    + escapeHtml(lastApiException.getResponseBodyAsString()) + "</p>";
            }

            return "<p>Gemini returned an empty response from all available models.</p>";
        } catch (Exception e) {
            return "<p>Error generating content: " + escapeHtml(e.getMessage()) + "</p>";
        }
    }

    private List<String> resolveModelNames(RestTemplate restTemplate) {
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

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
