package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.auth.UserPreferences;
import com._6.group4.smartcart.auth.UserPreferencesRepository;
import com._6.group4.smartcart.auth.UserRepository;
import com._6.group4.smartcart.grocery.PantryItem;
import com._6.group4.smartcart.grocery.PantryItemRepository;
import com._6.group4.smartcart.mealplanning.dto.GeminiMealPlanDto;
import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
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

    public MealPlanApiController(GeminiService geminiService,
                                 RecipeRepository recipeRepository,
                                 MealPlanRepository mealPlanRepository,
                                 UserRepository userRepository,
                                 UserPreferencesRepository preferencesRepository,
                                 PantryItemRepository pantryItemRepository) {
        this.geminiService = geminiService;
        this.recipeRepository = recipeRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.pantryItemRepository = pantryItemRepository;
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
    public ResponseEntity<?> generateMealPlan(@RequestBody(required = false) Map<String, String> body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return UNAUTHORIZED;
        String pantryIngredients = body != null ? body.getOrDefault("pantryIngredients", "") : "";

        UserPreferences prefs = preferencesRepository.findByUserId(userId).orElse(null);

        if (pantryIngredients.isBlank()) {
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
                prefs != null ? prefs.getDietaryRestrictions() : null,
                prefs != null ? prefs.getAllergies() : null,
                prefs != null && prefs.isRotateCuisines(),
                prefs != null ? prefs.getPreferredCuisines() : null,
                prefs != null ? prefs.getDislikedFoods() : null,
                prefs != null ? prefs.getMealSchedule() : null
        );
        if (dto == null || dto.meals() == null || dto.meals().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Could not generate a meal plan. Check your GEMINI_API_KEY."));
        }

        User user = userRepository.findById(userId).orElseThrow();
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        MealPlan plan = new MealPlan(user, monday);

        for (GeminiMealPlanDto.MealEntry entry : dto.meals()) {
            if (entry.recipe() == null) continue;
            Recipe recipe = persistRecipe(entry.recipe());
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
        Optional<MealPlan> planOpt = mealPlanRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
        if (planOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("items", List.of()));
        }

        MealPlan plan = planOpt.get();
        Map<String, GroceryItem> aggregated = new LinkedHashMap<>();

        for (MealPlanRecipe mpr : plan.getRecipes()) {
            for (RecipeIngredient ri : mpr.getRecipe().getIngredients()) {
                String key = ri.getIngredientName().toLowerCase();
                aggregated.merge(key, new GroceryItem(ri), GroceryItem::merge);
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (GroceryItem gi : aggregated.values()) {
            items.add(gi.toMap());
        }
        return ResponseEntity.ok(Map.of("items", items));
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

    private Recipe persistRecipe(GeminiRecipeDto dto) {
        Recipe recipe = new Recipe(dto.title() != null ? dto.title() : "Untitled Recipe");
        recipe.setInstructions(dto.instructions());
        recipe.setCookTimeMinutes(dto.cookTimeMinutes());
        recipe.setServings(dto.servings());
        recipe.setCuisine(dto.cuisine());
        recipe.setSource("gemini");

        if (dto.ingredients() != null) {
            for (Object ing : dto.ingredients()) {
                // Handle both string ingredients and object ingredients
                if (ing instanceof String ingStr) {
                    // Plain string ingredient (e.g., "Rolled Oats")
                    RecipeIngredient ri = new RecipeIngredient(recipe, ingStr.trim());
                    recipe.getIngredients().add(ri);
                } else if (ing instanceof java.util.Map<?, ?> ingMap) {
                    // Object ingredient with name, quantity, unit
                    String name = ingMap.get("name") != null ? ingMap.get("name").toString() : "unknown";
                    RecipeIngredient ri = new RecipeIngredient(recipe, name.trim());
                    
                    // Parse quantity (can be Double, String, or fraction)
                    Object qtyObj = ingMap.get("quantity");
                    if (qtyObj != null) {
                        Double qty = parseQuantity(qtyObj.toString());
                        if (qty != null) ri.setQuantity(qty);
                    }
                    
                    // Get unit
                    Object unitObj = ingMap.get("unit");
                    if (unitObj != null) {
                        ri.setUnit(unitObj.toString().trim());
                    }
                    
                    recipe.getIngredients().add(ri);
                }
            }
        }
        return recipeRepository.save(recipe);
    }

    private Double parseQuantity(String qtyStr) {
        if (qtyStr == null || qtyStr.isBlank()) return null;
        qtyStr = qtyStr.trim();
        
        try {
            // Try parsing as plain number (e.g., "1.5", "2")
            return Double.parseDouble(qtyStr);
        } catch (NumberFormatException e1) {
            try {
                // Try parsing as fraction (e.g., "1/2")
                if (qtyStr.contains("/")) {
                    String[] parts = qtyStr.split("/");
                    if (parts.length == 2) {
                        double num = Double.parseDouble(parts[0].trim());
                        double denom = Double.parseDouble(parts[1].trim());
                        if (denom != 0) return num / denom;
                    }
                }
                // Try extracting number from string like "1/2 tsp" (get the fraction part)
                String[] tokens = qtyStr.split("\\s+");
                if (tokens.length > 0) {
                    return parseQuantity(tokens[0]);
                }
            } catch (Exception e2) {
                // Could not parse; return null
            }
        }
        return null;
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

    /** Helper for aggregating grocery items by ingredient name. */
    private static class GroceryItem {
        String name;
        Double quantity;
        String unit;
        String category;

        GroceryItem(RecipeIngredient ri) {
            this.name = ri.getIngredientName();
            this.quantity = ri.getQuantity();
            this.unit = ri.getUnit();
            this.category = categorize(ri.getIngredientName());
        }

        GroceryItem merge(GroceryItem other) {
            if (this.quantity != null && other.quantity != null
                    && Objects.equals(this.unit, other.unit)) {
                this.quantity += other.quantity;
            }
            return this;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            String qty = quantity != null
                    ? (unit != null ? quantity + " " + unit : String.valueOf(quantity))
                    : (unit != null ? unit : "");
            m.put("quantity", qty);
            m.put("category", category);
            return m;
        }

        static String categorize(String name) {
            String lower = name.toLowerCase();
            if (lower.matches(".*(chicken|beef|salmon|turkey|pork|fish|shrimp|tofu|egg).*")) return "Protein";
            if (lower.matches(".*(milk|cheese|yogurt|cream|butter).*")) return "Dairy";
            if (lower.matches(".*(lettuce|tomato|onion|garlic|pepper|carrot|celery|spinach|avocado|lemon|lime|basil|parsley|cilantro|dill|ginger|broccoli|cabbage|cucumber).*"))
                return "Produce";
            return "Pantry";
        }
    }
}
