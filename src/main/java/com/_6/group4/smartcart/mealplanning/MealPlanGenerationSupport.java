package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.mealplanning.dto.GeminiMealPlanDto;
import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class MealPlanGenerationSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_SERVINGS = 2;
    private static final int MIN_SERVINGS = 1;
    private static final int MAX_SERVINGS = 6;
    private static final EnumSet<MealType> PLANNED_MEAL_TYPES =
        EnumSet.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER);

    private MealPlanGenerationSupport() {
    }

    static int normalizeServingSize(Integer requestedServingSize, Integer fallbackServingSize) {
        int candidate = requestedServingSize != null ? requestedServingSize
            : fallbackServingSize != null ? fallbackServingSize
            : DEFAULT_SERVINGS;

        if (candidate < MIN_SERVINGS) {
            return MIN_SERVINGS;
        }
        if (candidate > MAX_SERVINGS) {
            return MAX_SERVINGS;
        }
        return candidate;
    }

    static String normalizeSelectionList(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        Map<String, String> deduped = new LinkedHashMap<>();
        for (String token : raw.split(",")) {
            String trimmed = token == null ? null : token.trim();
            if (trimmed == null || trimmed.isBlank()) {
                continue;
            }

            String collapsed = trimmed.replaceAll("\\s+", " ");
            deduped.putIfAbsent(collapsed.toLowerCase(Locale.ROOT), collapsed);
        }

        return deduped.isEmpty() ? null : String.join(", ", deduped.values());
    }

    static Set<MealSlot> expectedSlots(String mealScheduleJson) {
        if (mealScheduleJson == null || mealScheduleJson.isBlank()) {
            return defaultSlots();
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(mealScheduleJson);
            if (root == null || root.isNull() || !root.isObject()) {
                return defaultSlots();
            }

            LinkedHashSet<MealSlot> slots = new LinkedHashSet<>();
            root.fields().forEachRemaining(entry -> {
                DayOfWeek day = parseDay(entry.getKey());
                if (day == null) {
                    return;
                }

                JsonNode value = entry.getValue();
                if (value == null || value.isNull()) {
                    return;
                }

                if (value.isArray()) {
                    for (JsonNode mealNode : value) {
                        MealType mealType = parseMealType(mealNode == null ? null : mealNode.asText());
                        if (mealType != null) {
                            slots.add(new MealSlot(day, mealType));
                        }
                    }
                } else {
                    MealType mealType = parseMealType(value.asText());
                    if (mealType != null) {
                        slots.add(new MealSlot(day, mealType));
                    }
                }
            });

            return slots.isEmpty() ? defaultSlots() : slots;
        } catch (JsonProcessingException ignored) {
            return defaultSlots();
        }
    }

    static String serializeSlots(Set<MealSlot> slots) {
        Map<String, List<String>> schedule = new LinkedHashMap<>();
        for (MealSlot slot : sortSlots(slots)) {
            schedule.computeIfAbsent(slot.dayOfWeek().name(), ignored -> new ArrayList<>())
                .add(slot.mealType().name());
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(schedule);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    static String describeSlots(Set<MealSlot> slots) {
        Map<DayOfWeek, List<String>> grouped = new LinkedHashMap<>();
        for (MealSlot slot : sortSlots(slots)) {
            grouped.computeIfAbsent(slot.dayOfWeek(), ignored -> new ArrayList<>())
                .add(slot.mealType().name());
        }

        List<String> lines = new ArrayList<>();
        for (Map.Entry<DayOfWeek, List<String>> entry : grouped.entrySet()) {
            lines.add(entry.getKey().name() + ": " + String.join(", ", entry.getValue()));
        }
        return String.join("; ", lines);
    }

    static ExtractedMeals extractValidMeals(GeminiMealPlanDto dto, Set<MealSlot> requestedSlots, int targetServings) {
        LinkedHashMap<MealSlot, GeminiMealPlanDto.MealEntry> accepted = new LinkedHashMap<>();
        if (dto != null && dto.meals() != null) {
            for (GeminiMealPlanDto.MealEntry entry : dto.meals()) {
                DayOfWeek day = parseDay(entry.dayOfWeek());
                MealType mealType = parseMealType(entry.mealType());
                if (day == null || mealType == null) {
                    continue;
                }

                MealSlot slot = new MealSlot(day, mealType);
                if (!requestedSlots.contains(slot) || accepted.containsKey(slot)) {
                    continue;
                }

                GeminiRecipeDto normalizedRecipe = normalizeRecipe(entry.recipe(), targetServings);
                if (normalizedRecipe == null) {
                    continue;
                }

                accepted.put(slot, new GeminiMealPlanDto.MealEntry(
                    slot.dayOfWeek().name(),
                    slot.mealType().name(),
                    normalizedRecipe
                ));
            }
        }

        List<MealSlot> missing = new ArrayList<>();
        for (MealSlot expected : sortSlots(requestedSlots)) {
            if (!accepted.containsKey(expected)) {
                missing.add(expected);
            }
        }

        return new ExtractedMeals(accepted, missing);
    }

    static GeminiMealPlanDto buildMealPlan(Map<MealSlot, GeminiMealPlanDto.MealEntry> entries) {
        List<GeminiMealPlanDto.MealEntry> orderedEntries = new ArrayList<>();
        for (MealSlot slot : sortSlots(entries.keySet())) {
            orderedEntries.add(entries.get(slot));
        }
        return new GeminiMealPlanDto(orderedEntries);
    }

    private static GeminiRecipeDto normalizeRecipe(GeminiRecipeDto recipe, int targetServings) {
        if (recipe == null) {
            return null;
        }

        String title = cleanText(recipe.title());
        String instructions = cleanText(recipe.instructions());
        List<GeminiRecipeDto.IngredientDto> ingredients = recipe.normalizedIngredients();

        if (title == null || instructions == null || ingredients.isEmpty()) {
            return null;
        }

        Integer servings = recipe.servings();
        if (servings == null || servings <= 0) {
            servings = targetServings;
        }

        Integer cookTimeMinutes = recipe.cookTimeMinutes();
        if (cookTimeMinutes != null && cookTimeMinutes <= 0) {
            cookTimeMinutes = null;
        }

        return new GeminiRecipeDto(
            title,
            cleanText(recipe.cuisine()),
            cookTimeMinutes,
            servings,
            instructions,
            new ArrayList<>(ingredients)
        );
    }

    private static String cleanText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static DayOfWeek parseDay(String raw) {
        String token = normalizeEnumToken(raw);
        if (token == null) {
            return null;
        }

        try {
            return DayOfWeek.valueOf(token);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static MealType parseMealType(String raw) {
        String token = normalizeEnumToken(raw);
        if (token == null) {
            return null;
        }

        try {
            MealType mealType = MealType.valueOf(token);
            return PLANNED_MEAL_TYPES.contains(mealType) ? mealType : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalizeEnumToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim()
            .replace('-', '_')
            .replace(' ', '_')
            .toUpperCase(Locale.ROOT);
    }

    private static LinkedHashSet<MealSlot> defaultSlots() {
        LinkedHashSet<MealSlot> slots = new LinkedHashSet<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (MealType mealType : PLANNED_MEAL_TYPES) {
                slots.add(new MealSlot(day, mealType));
            }
        }
        return slots;
    }

    private static List<MealSlot> sortSlots(Set<MealSlot> slots) {
        List<MealSlot> ordered = new ArrayList<>(slots);
        ordered.sort(Comparator
            .comparingInt((MealSlot slot) -> slot.dayOfWeek().ordinal())
            .thenComparingInt(slot -> slot.mealType().ordinal()));
        return ordered;
    }

    record MealSlot(DayOfWeek dayOfWeek, MealType mealType) {
    }

    record ExtractedMeals(
        Map<MealSlot, GeminiMealPlanDto.MealEntry> acceptedMeals,
        List<MealSlot> missingSlots
    ) {
        boolean isComplete() {
            return missingSlots.isEmpty() && !acceptedMeals.isEmpty();
        }
    }
}
