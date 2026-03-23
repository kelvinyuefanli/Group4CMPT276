package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.auth.SessionKeys;
import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.auth.UserPreferences;
import com._6.group4.smartcart.auth.UserPreferencesRepository;
import com._6.group4.smartcart.auth.UserRepository;
import com._6.group4.smartcart.grocery.GroceryAggregationService;
import com._6.group4.smartcart.grocery.IngredientNormalizer;
import com._6.group4.smartcart.grocery.PantryItem;
import com._6.group4.smartcart.grocery.PantryItemUpdateRequest;
import com._6.group4.smartcart.grocery.PantryItemRepository;
import com._6.group4.smartcart.mealplanning.dto.GeminiMealPlanDto;
import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
import com._6.group4.smartcart.mealplanning.dto.MealPlanGenerationRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RestController
@RequestMapping("/api")
public class MealPlanApiController {

    private static final Logger log = LoggerFactory.getLogger(MealPlanApiController.class);

    private final GeminiService geminiService;
    private final RecipeRepository recipeRepository;
    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final PantryItemRepository pantryItemRepository;
    private final GroceryAggregationService groceryAggregationService;

    public MealPlanApiController(GeminiService geminiService,
                                 RecipeRepository recipeRepository,
                                 MealPlanRepository mealPlanRepository,
                                 UserRepository userRepository,
                                 UserPreferencesRepository preferencesRepository,
                                 PantryItemRepository pantryItemRepository,
                                 GroceryAggregationService groceryAggregationService) {
        this.geminiService = geminiService;
        this.recipeRepository = recipeRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.groceryAggregationService = groceryAggregationService;
    }

    /** Resolves the current user ID from session. Returns null if not authenticated. */
    private Long getCurrentUserId(HttpSession session) {
        Object id = session != null ? session.getAttribute(SessionKeys.USER_ID) : null;
        if (id instanceof Long) return (Long) id;
        if (id instanceof Number) return ((Number) id).longValue();
        return null;
    }

    private static final ResponseEntity<?> UNAUTHORIZED =
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));

    // ---- Meal Plan --------------------------------------------------------

    @GetMapping("/meal-plan")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMealPlan(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;
        return mealPlanRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(plan -> ResponseEntity.ok(toMealPlanResponse(plan)))
                .orElse(ResponseEntity.ok(Map.of("meals", List.of())));
    }

    @PostMapping("/meal-plan/generate")
    @Transactional
    public ResponseEntity<?> generateMealPlan(@RequestBody(required = false) MealPlanGenerationRequest body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;
        String pantryIngredients = body != null ? normalizeText(body.pantryIngredients()) : null;

        UserPreferences prefs = preferencesRepository.findByUserId(userId)
            .orElseGet(() -> {
                User user = userRepository.findById(userId).orElseThrow();
                return preferencesRepository.save(new UserPreferences(user));
            });

        Integer requestedServingSize = body != null ? body.servingSize() : null;
        int effectiveServingSize = MealPlanGenerationSupport.normalizeServingSize(
            requestedServingSize,
            prefs.getServingSize()
        );
        String effectiveDietaryRestrictions = normalizedOverride(
            body != null ? body.dietaryRestrictions() : null,
            prefs.getDietaryRestrictions()
        );
        String effectiveAllergies = normalizedOverride(
            body != null ? body.allergies() : null,
            prefs.getAllergies()
        );
        String effectivePreferredCuisines = normalizedOverride(
            body != null ? body.preferredCuisines() : null,
            prefs.getPreferredCuisines()
        );
        String effectiveDislikedFoods = normalizedOverride(
            body != null ? body.dislikedFoods() : null,
            prefs.getDislikedFoods()
        );
        String effectiveMealSchedule = normalizeText(
            body != null && body.mealSchedule() != null ? body.mealSchedule() : prefs.getMealSchedule()
        );
        boolean effectiveRotateCuisines =
            body != null && body.rotateCuisines() != null ? body.rotateCuisines() : prefs.isRotateCuisines();

        if (body != null) {
            prefs.setServingSize(effectiveServingSize);
            prefs.setDietaryRestrictions(effectiveDietaryRestrictions);
            prefs.setAllergies(effectiveAllergies);
            prefs.setPreferredCuisines(effectivePreferredCuisines);
            prefs.setRotateCuisines(effectiveRotateCuisines);
            prefs.setDislikedFoods(effectiveDislikedFoods);
            prefs.setMealSchedule(effectiveMealSchedule);
            preferencesRepository.save(prefs);
        }

        if (pantryIngredients == null || pantryIngredients.isBlank()) {
            List<PantryItem> pantryItems = pantryItemRepository.findAllByUserIdOrderByIngredientName(userId);
            if (!pantryItems.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (PantryItem pi : pantryItems) {
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(pi.getIngredientName());
                }
                pantryIngredients = sb.toString();
            }
        }

        GeminiMealPlanDto dto = geminiService.generateMealPlan(
                pantryIngredients,
                effectiveDietaryRestrictions,
                effectiveAllergies,
                effectiveRotateCuisines,
                effectivePreferredCuisines,
                effectiveDislikedFoods,
                effectiveMealSchedule,
                prefs.getPreferredProteins(),
                prefs.getPreferredVegetables(),
                prefs.getPreferredFruits(),
                effectiveServingSize
        );
        if (dto == null || dto.meals() == null || dto.meals().isEmpty()) {
            String errorMsg = geminiService.getLastErrorMessage();
            if (errorMsg == null) errorMsg = "Could not generate a meal plan. Please try again.";
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User account not found. Please log in again."));
        }
        User user = userOpt.get();
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        MealPlan plan = new MealPlan(user, monday);
        Set<MealPlanGenerationSupport.MealSlot> expectedSlots =
            MealPlanGenerationSupport.expectedSlots(effectiveMealSchedule);
        MealPlanGenerationSupport.ExtractedMeals extracted =
            MealPlanGenerationSupport.extractValidMeals(dto, expectedSlots, effectiveServingSize);

        if (!extracted.isComplete()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Generated plan was incomplete for the selected preferences. Please try again."));
        }

        // Parse allergen list for post-generation verification
        Set<String> allergenSet = parseAllergenSet(effectiveAllergies);
        List<String> removedMeals = new ArrayList<>();

        for (GeminiMealPlanDto.MealEntry entry : MealPlanGenerationSupport.buildMealPlan(extracted.acceptedMeals()).meals()) {
            if (entry.recipe() == null) continue;
            try {
                DayOfWeek day = DayOfWeek.valueOf(entry.dayOfWeek());
                MealType meal = MealType.valueOf(entry.mealType());

                // Allergy verification: check recipe ingredients against allergen list
                if (!allergenSet.isEmpty() && recipeContainsAllergen(entry.recipe(), allergenSet)) {
                    log.warn("Allergy detected in {} {} recipe '{}' — attempting re-generation",
                            entry.dayOfWeek(), entry.mealType(),
                            entry.recipe().title());

                    // Re-generate this single slot with a stronger allergy prompt
                    GeminiRecipeDto replacement = geminiService.generateSingleMeal(
                            entry.dayOfWeek(), entry.mealType(),
                            effectiveAllergies, effectiveDietaryRestrictions, effectivePreferredCuisines);

                    if (replacement != null && !recipeContainsAllergen(replacement, allergenSet)) {
                        Recipe recipe = persistRecipe(replacement, effectiveServingSize);
                        plan.getRecipes().add(new MealPlanRecipe(plan, recipe, day, meal));
                    } else {
                        // Re-generation also failed — remove this slot entirely
                        removedMeals.add(entry.dayOfWeek() + " " + entry.mealType());
                        log.warn("Re-generation still contained allergen for {} {} — slot removed",
                                entry.dayOfWeek(), entry.mealType());
                    }
                } else {
                    Recipe recipe = persistRecipe(entry.recipe(), effectiveServingSize);
                    plan.getRecipes().add(new MealPlanRecipe(plan, recipe, day, meal));
                }
            } catch (IllegalArgumentException ignored) {
                // skip entries with invalid day/meal values from Gemini
            }
        }

        mealPlanRepository.save(plan);
        Map<String, Object> response = toMealPlanResponse(plan);
        if (!removedMeals.isEmpty()) {
            response.put("allergyWarnings", removedMeals.stream()
                    .map(slot -> slot + " was removed because it contained an allergen")
                    .toList());
        }
        return ResponseEntity.ok(response);
    }

    // ---- Meal Swap --------------------------------------------------------

    /**
     * Swap one or more meal slots in the current plan with freshly generated recipes.
     * Accepts: { "slots": [{"dayOfWeek":"MONDAY","mealType":"DINNER"}, ...] }
     */
    @PostMapping("/meal-plan/swap")
    @Transactional
    public ResponseEntity<?> swapMeals(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;

        @SuppressWarnings("unchecked")
        List<Map<String, String>> slots = (List<Map<String, String>>) body.get("slots");
        if (slots == null || slots.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No meal slots specified"));
        }

        Optional<MealPlan> planOpt = mealPlanRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
        if (planOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No meal plan exists. Generate one first."));
        }
        MealPlan plan = planOpt.get();

        UserPreferences prefs = preferencesRepository.findByUserId(userId).orElse(null);
        String allergies = prefs != null ? prefs.getAllergies() : null;
        String dietaryRestrictions = prefs != null ? prefs.getDietaryRestrictions() : null;
        String preferredCuisines = prefs != null ? prefs.getPreferredCuisines() : null;
        int effectiveServingSize = prefs != null ? prefs.getServingSize() : 2;

        Set<String> allergenSet = parseAllergenSet(allergies);
        List<String> swapped = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (Map<String, String> slot : slots) {
            String dayStr = slot.get("dayOfWeek");
            String mealStr = slot.get("mealType");
            if (dayStr == null || mealStr == null) continue;

            try {
                DayOfWeek day = DayOfWeek.valueOf(dayStr);
                MealType meal = MealType.valueOf(mealStr);

                // Generate a replacement recipe
                GeminiRecipeDto replacement = geminiService.generateSingleMeal(
                        dayStr, mealStr, allergies, dietaryRestrictions, preferredCuisines);

                if (replacement == null) {
                    failed.add(dayStr + " " + mealStr);
                    continue;
                }

                // Allergy check on the replacement
                if (!allergenSet.isEmpty() && recipeContainsAllergen(replacement, allergenSet)) {
                    // Try once more
                    replacement = geminiService.generateSingleMeal(
                            dayStr, mealStr, allergies, dietaryRestrictions, preferredCuisines);
                    if (replacement == null || recipeContainsAllergen(replacement, allergenSet)) {
                        failed.add(dayStr + " " + mealStr + " (allergen detected)");
                        continue;
                    }
                }

                Recipe newRecipe = persistRecipe(replacement, effectiveServingSize);

                // Remove old recipe for this slot
                plan.getRecipes().removeIf(mpr ->
                        mpr.getDayOfWeek() == day && mpr.getMealType() == meal);

                // Add new recipe
                plan.getRecipes().add(new MealPlanRecipe(plan, newRecipe, day, meal));
                swapped.add(dayStr + " " + mealStr);

            } catch (IllegalArgumentException e) {
                failed.add(dayStr + " " + mealStr + " (invalid)");
            }
        }

        mealPlanRepository.save(plan);
        Map<String, Object> response = toMealPlanResponse(plan);
        response.put("swapped", swapped);
        if (!failed.isEmpty()) {
            response.put("swapFailed", failed);
        }
        return ResponseEntity.ok(response);
    }

    // ---- Recipes ----------------------------------------------------------

    @GetMapping("/recipes/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRecipe(@PathVariable Long id) {
        return recipeRepository.findById(id)
                .map(r -> ResponseEntity.ok(toRecipeResponse(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Grocery List -----------------------------------------------------

    @GetMapping("/grocery-list")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getGroceryList(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;

        List<PantryItem> pantryItems = pantryItemRepository.findAllByUserIdOrderByIngredientName(userId);
        Optional<MealPlan> planOpt = mealPlanRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(
                groceryAggregationService
                        .buildGroceryList(planOpt.orElse(null), pantryItems)
                        .toResponseMap()
        );
    }

    // ---- Preferences ------------------------------------------------------

    @GetMapping("/preferences")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPreferences(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;
        return preferencesRepository.findByUserId(userId)
                .map(p -> {
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("servingSize", p.getServingSize());
                    resp.put("dietaryRestrictions", p.getDietaryRestrictions());
                    resp.put("allergies", p.getAllergies());
                    resp.put("preferredCuisines", p.getPreferredCuisines());
                    resp.put("rotateCuisines", p.isRotateCuisines());
                    resp.put("dislikedFoods", p.getDislikedFoods());
                    resp.put("mealSchedule", p.getMealSchedule());
                    resp.put("preferredProteins", p.getPreferredProteins());
                    resp.put("preferredVegetables", p.getPreferredVegetables());
                    resp.put("preferredFruits", p.getPreferredFruits());
                    resp.put("onboardingCompleted", p.isOnboardingCompleted());
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/preferences")
    @Transactional
    public ResponseEntity<?> updatePreferences(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User account not found."));
        }

        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> preferencesRepository.save(new UserPreferences(userOpt.get())));

        try {
            if (body.containsKey("servingSize")) {
                Object val = body.get("servingSize");
                if (val instanceof Number n) {
                    prefs.setServingSize(n.intValue());
                } else {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "servingSize must be a number"));
                }
            }
            if (body.containsKey("dietaryRestrictions")) {
                prefs.setDietaryRestrictions(toString(body.get("dietaryRestrictions")));
            }
            if (body.containsKey("allergies")) {
                prefs.setAllergies(toString(body.get("allergies")));
            }
            if (body.containsKey("preferredCuisines")) {
                prefs.setPreferredCuisines(toString(body.get("preferredCuisines")));
            }
            if (body.containsKey("rotateCuisines")) {
                Object val = body.get("rotateCuisines");
                if (val instanceof Boolean b) {
                    prefs.setRotateCuisines(b);
                }
            }
            if (body.containsKey("dislikedFoods")) {
                prefs.setDislikedFoods(toString(body.get("dislikedFoods")));
            }
            if (body.containsKey("mealSchedule")) {
                prefs.setMealSchedule(toString(body.get("mealSchedule")));
            }
            if (body.containsKey("preferredProteins")) {
                prefs.setPreferredProteins(toString(body.get("preferredProteins")));
            }
            if (body.containsKey("preferredVegetables")) {
                prefs.setPreferredVegetables(toString(body.get("preferredVegetables")));
            }
            if (body.containsKey("preferredFruits")) {
                prefs.setPreferredFruits(toString(body.get("preferredFruits")));
            }
            if (body.containsKey("onboardingCompleted")) {
                Object val = body.get("onboardingCompleted");
                if (val instanceof Boolean b) {
                    prefs.setOnboardingCompleted(b);
                }
            }
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid field type in preferences update"));
        }

        preferencesRepository.save(prefs);
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    private static String toString(Object val) {
        return val == null ? null : val.toString();
    }

    // ---- Pantry -----------------------------------------------------------

    @GetMapping("/pantry")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPantry(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;
        List<PantryItem> items = pantryItemRepository.findAllByUserIdOrderByIngredientName(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PantryItem pi : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", pi.getId());
            m.put("name", pi.getIngredientName());
            m.put("canonicalName", pi.getCanonicalName());
            m.put("quantity", pi.getQuantity());
            m.put("unit", pi.getUnit());
            result.add(m);
        }
        return ResponseEntity.ok(Map.of("items", result));
    }

    @PostMapping("/pantry")
    @Transactional
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> savePantry(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User account not found."));
        }
        User user = userOpt.get();
        List<String> itemNames = (List<String>) body.getOrDefault("items", List.of());

        pantryItemRepository.deleteAllByUserId(userId);

        List<PantryItem> saved = new ArrayList<>();
        for (String name : itemNames) {
            if (name != null && !name.isBlank()) {
                PantryItem pantryItem = new PantryItem(user, name.trim());
                saved.add(pantryItemRepository.save(pantryItem));
            }
        }
        return ResponseEntity.ok(Map.of("count", saved.size()));
    }

    @PutMapping("/pantry/item")
    @Transactional
    public ResponseEntity<?> updatePantryItem(@RequestBody PantryItemUpdateRequest body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;

        String displayName = normalizeText(body.name());
        String requestedCanonicalName = normalizeText(body.canonicalName());
        String canonicalName = IngredientNormalizer.canonicalizeName(
                requestedCanonicalName != null ? requestedCanonicalName : displayName
        );

        if (canonicalName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A pantry item name is required."));
        }

        boolean covered = Boolean.TRUE.equals(body.covered());
        String normalizedUnit = GroceryAggregationService.normalizeStoredUnit(body.unit());
        Double requestedQuantity = body.quantity();
        boolean numericRequest = requestedQuantity != null || normalizedUnit != null;
        List<PantryItem> existingItems = pantryItemRepository.findAllByUserIdOrderByIngredientName(userId);

        if (!covered) {
            removePantryEntry(existingItems, canonicalName, normalizedUnit, numericRequest);
            return ResponseEntity.ok(Map.of("status", "removed"));
        }

        User user = userRepository.findById(userId).orElseThrow();
        if (!numericRequest) {
            removeAllPantryEntries(existingItems, canonicalName);
            PantryItem pantryItem = new PantryItem(user, displayName != null ? displayName : canonicalName);
            pantryItem.setQuantity(null);
            pantryItem.setUnit(null);
            pantryItemRepository.save(pantryItem);
            return ResponseEntity.ok(Map.of("status", "saved"));
        }

        if (requestedQuantity == null || requestedQuantity <= 0d) {
            removePantryEntry(existingItems, canonicalName, normalizedUnit, true);
            return ResponseEntity.ok(Map.of("status", "removed"));
        }

        removeBooleanPantryEntries(existingItems, canonicalName);

        List<PantryItem> numericMatches = findNumericPantryItems(existingItems, canonicalName, normalizedUnit);
        PantryItem pantryItem = numericMatches.isEmpty()
                ? new PantryItem(user, displayName != null ? displayName : canonicalName)
                : numericMatches.get(0);
        if (numericMatches.size() > 1) {
            pantryItemRepository.deleteAll(numericMatches.subList(1, numericMatches.size()));
        }
        pantryItem.setUser(user);
        pantryItem.setIngredientName(displayName != null ? displayName : canonicalName);
        pantryItem.setQuantity(requestedQuantity);
        pantryItem.setUnit(normalizedUnit);
        pantryItemRepository.save(pantryItem);
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    @DeleteMapping("/pantry/{id}")
    @Transactional
    public ResponseEntity<?> deletePantryItem(@PathVariable Long id, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;
        return pantryItemRepository.findById(id)
                .filter(item -> item.getUser().getId().equals(userId))
                .map(item -> {
                    pantryItemRepository.delete(item);
                    return ResponseEntity.ok(Map.of("status", "deleted"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Helpers ----------------------------------------------------------

    private Recipe persistRecipe(GeminiRecipeDto dto, int defaultServings) {
        Recipe recipe = new Recipe(dto.title() != null ? dto.title() : "Untitled Recipe");
        recipe.setInstructions(dto.instructions());
        recipe.setCookTimeMinutes(dto.cookTimeMinutes());
        recipe.setServings(dto.servings() != null && dto.servings() > 0 ? dto.servings() : defaultServings);
        recipe.setCuisine(dto.cuisine());
        recipe.setSource("gemini");

        for (GeminiRecipeDto.IngredientDto ingredient : dto.normalizedIngredients()) {
            RecipeIngredient ri = new RecipeIngredient(recipe, ingredient.safeName());
            ri.setCanonicalName(IngredientNormalizer.canonicalizeName(ingredient.safeName()));

            Double quantity = ingredient.quantityAsDouble();
            if (quantity != null) {
                ri.setQuantity(quantity);
            }

            String unit = ingredient.safeUnit();
            if (unit != null) {
                ri.setUnit(unit);
            }

            recipe.getIngredients().add(ri);
        }
        return recipeRepository.save(recipe);
    }

    private String normalizedOverride(String requestedValue, String storedValue) {
        String normalizedRequested = MealPlanGenerationSupport.normalizeSelectionList(requestedValue);
        if (requestedValue != null) {
            return normalizedRequested;
        }
        return MealPlanGenerationSupport.normalizeSelectionList(storedValue);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private List<PantryItem> findNumericPantryItems(Collection<PantryItem> pantryItems, String canonicalName, String unit) {
        List<PantryItem> matches = new ArrayList<>();
        for (PantryItem pantryItem : pantryItems) {
            if (pantryItem.getQuantity() == null) {
                continue;
            }
            if (!Objects.equals(canonicalName, pantryItem.getCanonicalName())) {
                continue;
            }
            if (sameNormalizedUnit(pantryItem.getUnit(), unit)) {
                matches.add(pantryItem);
            }
        }
        return matches;
    }

    private void removePantryEntry(Collection<PantryItem> pantryItems, String canonicalName, String unit, boolean numericRequest) {
        List<PantryItem> matches = new ArrayList<>();
        for (PantryItem pantryItem : pantryItems) {
            if (!Objects.equals(canonicalName, pantryItem.getCanonicalName())) {
                continue;
            }
            if (!numericRequest && pantryItem.getQuantity() == null) {
                matches.add(pantryItem);
                continue;
            }
            if (numericRequest && pantryItem.getQuantity() != null && sameNormalizedUnit(pantryItem.getUnit(), unit)) {
                matches.add(pantryItem);
            }
        }
        if (!matches.isEmpty()) {
            pantryItemRepository.deleteAll(matches);
        }
    }

    private void removeBooleanPantryEntries(Collection<PantryItem> pantryItems, String canonicalName) {
        removePantryEntry(pantryItems, canonicalName, null, false);
    }

    private void removeAllPantryEntries(Collection<PantryItem> pantryItems, String canonicalName) {
        List<PantryItem> matches = new ArrayList<>();
        for (PantryItem pantryItem : pantryItems) {
            if (Objects.equals(canonicalName, pantryItem.getCanonicalName())) {
                matches.add(pantryItem);
            }
        }
        if (!matches.isEmpty()) {
            pantryItemRepository.deleteAll(matches);
        }
    }

    private boolean sameNormalizedUnit(String left, String right) {
        String normalizedLeft = GroceryAggregationService.normalizeStoredUnit(left);
        String normalizedRight = GroceryAggregationService.normalizeStoredUnit(right);
        return Objects.equals(normalizedLeft, normalizedRight);
    }

    private Map<String, Object> toMealPlanResponse(MealPlan plan) {
        List<Map<String, Object>> meals = new ArrayList<>();
        for (MealPlanRecipe mpr : plan.getRecipes()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("dayOfWeek", mpr.getDayOfWeek().name());
            entry.put("mealType", mpr.getMealType().name());
            entry.put("recipeId", mpr.getRecipe().getId());
            entry.put("recipeName", mpr.getRecipe().getTitle());
            meals.add(entry);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", plan.getId());
        resp.put("weekStartDate", plan.getWeekStartDate().toString());
        resp.put("meals", meals);
        return resp;
    }

    private Map<String, Object> toRecipeResponse(Recipe r) {
        List<Map<String, Object>> ings = new ArrayList<>();
        for (RecipeIngredient ri : r.getIngredients()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", ri.getIngredientName());
            m.put("quantity", ri.getQuantity());
            m.put("unit", ri.getUnit());
            ings.add(m);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", r.getId());
        resp.put("title", r.getTitle());
        resp.put("cuisine", r.getCuisine());
        resp.put("cookTimeMinutes", r.getCookTimeMinutes());
        resp.put("servings", r.getServings());
        resp.put("instructions", r.getInstructions());
        resp.put("ingredients", ings);
        return resp;
    }

    // ---- Allergy Verification -----------------------------------------------

    /**
     * Parses the user's allergy string into a set of lowercase allergen keywords.
     * The allergy field is typically comma-separated (e.g., "Peanuts, Tree Nuts, Shellfish").
     */
    static Set<String> parseAllergenSet(String allergies) {
        if (allergies == null || allergies.isBlank()) return Set.of();
        Set<String> result = new HashSet<>();
        for (String allergen : allergies.split("[,;]+")) {
            String trimmed = allergen.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Checks if any ingredient in the recipe contains an allergen keyword.
     * Uses substring matching to catch derivatives (e.g., "peanut butter" matches "peanut").
     */
    static boolean recipeContainsAllergen(GeminiRecipeDto recipe, Set<String> allergens) {
        if (recipe.ingredients() == null || allergens.isEmpty()) return false;
        for (GeminiRecipeDto.IngredientDto ing : recipe.normalizedIngredients()) {
            String ingredientName = ing.safeName();
            if (ingredientName == null) continue;
            String lower = ingredientName.toLowerCase();
            for (String allergen : allergens) {
                if (lower.contains(allergen)) {
                    return true;
                }
            }
        }
        return false;
    }
}
