# SmartCart — Deferred Work

## P1 — High Priority

### Instacart API Integration (Epic 4)
- **What:** Match grocery list items to real Instacart products and generate a shopping link.
- **Why:** Course requirement for 2 external APIs. Currently only Gemini is integrated.
- **Context:** The `instacart` package exists but is empty. Grocery aggregation works.
  Use Instacart Developer Platform API: GET /idp/v1/retailers for nearby retailers,
  POST /idp/v1/products/recipe for product matching.
- **Depends on:** Grocery aggregation working (it does).
- **Effort:** L (human: ~1 week) → M (CC: ~1 hour)

### Gemini Allergy Verification — Expand Allergen Database
- **What:** The current allergy check uses simple substring matching. Expand to handle
  allergen derivatives (e.g., "whey" → dairy, "lecithin" → soy).
- **Why:** Substring matching misses indirect allergens.
- **Context:** Current implementation is in `MealPlanApiController.recipeContainsAllergen()`.
  Consider a proper allergen synonym map.
- **Depends on:** Allergy verification (done).
- **Effort:** S (CC: ~15 min)

## P2 — Medium Priority

### Cache Gemini Model List
- **What:** `GeminiService.resolveModelNames()` makes an HTTP call to list available models
  on EVERY meal plan generation request. Cache for 1 hour.
- **Why:** Adds ~200-500ms latency and an extra API call per generation.
- **Context:** Simple in-memory cache with TTL. Use `@Cacheable` or a manual timestamp check.
- **Depends on:** Nothing.
- **Effort:** S (CC: ~10 min)

### Login Rate Limiting
- **What:** Limit failed login attempts to 5 per email per 15 minutes.
- **Why:** Prevents brute-force password guessing.
- **Context:** Use an in-memory `ConcurrentHashMap<String, AttemptRecord>` in `AuthService`.
- **Depends on:** Nothing.
- **Effort:** S (CC: ~10 min)

## P3 — Low Priority

### Full Spring Security Migration
- **What:** Replace the manual session auth + bridge filter with Spring Security's
  built-in `formLogin()` + `UserDetailsService` pattern.
- **Why:** The current bridge filter works but is not the standard Spring Security pattern.
  Full migration would enable features like remember-me, OAuth, etc.
- **Context:** Currently using `SessionAuthFilter` to bridge manual session → Spring Security.
- **Depends on:** Nothing, but large scope.
- **Effort:** M (CC: ~30 min)
