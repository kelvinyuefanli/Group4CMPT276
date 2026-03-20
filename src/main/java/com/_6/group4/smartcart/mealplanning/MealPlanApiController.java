package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.auth.UserPreferences;
import com._6.group4.smartcart.auth.UserPreferencesRepository;
import com._6.group4.smartcart.auth.UserRepository;
import com._6.group4.smartcart.grocery.GroceryAggregationService;
import com._6.group4.smartcart.grocery.IngredientNormalizer;
import com._6.group4.smartcart.grocery.PantryItem;
import com._6.group4.smartcart.grocery.PantryItemRepository;
import com._6.group4.smartcart.mealplanning.dto.GeminiMealPlanDto;
import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
import com._6.group4.smartcart.mealplanning.dto.MealPlanGenerationRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RestController
@RequestMapping("/api")
public class MealPlanApiController {

    private static final String SESSION_USER_ID = "USER_ID";

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
        Object id = session != null ? session.getAttribute(SESSION_USER_ID) : null;
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
                effectiveServingSize,
                effectiveDietaryRestrictions,
                effectiveAllergies,
                effectiveRotateCuisines,
                effectivePreferredCuisines,
                effectiveDislikedFoods,
                effectiveMealSchedule
        );
        if (dto == null || dto.meals() == null || dto.meals().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Could not generate a complete meal plan for the selected preferences. Please try again."));
        }

        User user = userRepository.findById(userId).orElseThrow();
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

        for (GeminiMealPlanDto.MealEntry entry : MealPlanGenerationSupport.buildMealPlan(extracted.acceptedMeals()).meals()) {
            if (entry.recipe() == null) continue;
            Recipe recipe = persistRecipe(entry.recipe(), effectiveServingSize);
            try {
                DayOfWeek day = DayOfWeek.valueOf(entry.dayOfWeek());
                MealType meal = MealType.valueOf(entry.mealType());
                MealPlanRecipe mpr = new MealPlanRecipe(plan, recipe, day, meal);
                plan.getRecipes().add(mpr);
            } catch (IllegalArgumentException ignored) {
                // skip entries with invalid day/meal values from Gemini
            }
        }

        mealPlanRepository.save(plan);
        return ResponseEntity.ok(toMealPlanResponse(plan));
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
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow();
                    return preferencesRepository.save(new UserPreferences(user));
                });
        if (body.containsKey("servingSize")) {
            prefs.setServingSize(((Number) body.get("servingSize")).intValue());
        }
        if (body.containsKey("dietaryRestrictions")) {
            prefs.setDietaryRestrictions((String) body.get("dietaryRestrictions"));
        }
        if (body.containsKey("allergies")) {
            prefs.setAllergies((String) body.get("allergies"));
        }
        if (body.containsKey("preferredCuisines")) {
            prefs.setPreferredCuisines((String) body.get("preferredCuisines"));
        }
        if (body.containsKey("rotateCuisines")) {
            prefs.setRotateCuisines((Boolean) body.get("rotateCuisines"));
        }
        if (body.containsKey("dislikedFoods")) {
            prefs.setDislikedFoods((String) body.get("dislikedFoods"));
        }
        if (body.containsKey("mealSchedule")) {
            prefs.setMealSchedule((String) body.get("mealSchedule"));
        }
        if (body.containsKey("onboardingCompleted")) {
            prefs.setOnboardingCompleted((Boolean) body.get("onboardingCompleted"));
        }
        preferencesRepository.save(prefs);
        return ResponseEntity.ok(Map.of("status", "updated"));
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
        User user = userRepository.findById(userId).orElseThrow();
        List<String> itemNames = (List<String>) body.getOrDefault("items", List.of());

        pantryItemRepository.deleteAllByUserId(userId);

        List<PantryItem> saved = new ArrayList<>();
        for (String name : itemNames) {
            if (name != null && !name.isBlank()) {
                saved.add(pantryItemRepository.save(new PantryItem(user, name.trim())));
            }
        }
        return ResponseEntity.ok(Map.of("count", saved.size()));
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
}
