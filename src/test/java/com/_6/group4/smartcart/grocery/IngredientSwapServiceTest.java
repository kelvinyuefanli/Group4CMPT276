package com._6.group4.smartcart.grocery;

import com._6.group4.smartcart.grocery.IngredientSwapService.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IngredientSwapServiceTest {

    // ---- Categorization ----
    @Test void categorize_chickenIsPoultry() {
        assertEquals(Category.POULTRY, IngredientSwapService.categorize("chicken thighs"));
    }

    @Test void categorize_salmonIsSeafood() {
        assertEquals(Category.SEAFOOD, IngredientSwapService.categorize("salmon"));
    }

    @Test void categorize_broccoliIsCruciferous() {
        assertEquals(Category.CRUCIFEROUS, IngredientSwapService.categorize("broccoli"));
    }

    @Test void categorize_fuzzyMatch() {
        assertEquals(Category.POULTRY, IngredientSwapService.categorize("boneless skinless chicken breast"));
    }

    @Test void categorize_unknownItem() {
        assertEquals(Category.UNKNOWN, IngredientSwapService.categorize("zzz mystery food"));
    }

    @Test void categorize_null() {
        assertEquals(Category.UNKNOWN, IngredientSwapService.categorize(null));
    }

    // ---- Alternatives ----
    @Test void alternatives_chickenHasOtherProteins() {
        List<SwapOption> alts = IngredientSwapService.getAlternatives("chicken thighs");
        assertFalse(alts.isEmpty());
        // Should include other poultry + cross-category proteins
        List<String> names = alts.stream().map(SwapOption::name).toList();
        assertTrue(names.contains("chicken breast") || names.contains("ground chicken"),
                "Should have other poultry options");
    }

    @Test void alternatives_chickenIncludesCrossCategory() {
        List<SwapOption> alts = IngredientSwapService.getAlternatives("chicken thighs");
        List<String> names = alts.stream().map(SwapOption::name).toList();
        // Should have red meat and seafood as cross-category swaps
        boolean hasRedMeat = names.stream().anyMatch(n -> n.contains("beef") || n.contains("pork"));
        boolean hasSeafood = names.stream().anyMatch(n -> n.contains("salmon") || n.contains("shrimp"));
        assertTrue(hasRedMeat, "Should offer red meat alternatives");
        assertTrue(hasSeafood, "Should offer seafood alternatives");
    }

    @Test void alternatives_excludesSelf() {
        List<SwapOption> alts = IngredientSwapService.getAlternatives("salmon");
        List<String> names = alts.stream().map(SwapOption::name).toList();
        assertFalse(names.contains("salmon"), "Should not include self as alternative");
    }

    @Test void alternatives_broccoliHasCruciferousOptions() {
        List<SwapOption> alts = IngredientSwapService.getAlternatives("broccoli");
        List<String> names = alts.stream().map(SwapOption::name).toList();
        assertTrue(names.contains("cauliflower") || names.contains("brussels sprouts"),
                "Should have other cruciferous veggies");
    }

    @Test void alternatives_unknownReturnsEmpty() {
        List<SwapOption> alts = IngredientSwapService.getAlternatives("zzz unknown");
        assertTrue(alts.isEmpty());
    }

    @Test void alternatives_nullReturnsEmpty() {
        assertTrue(IngredientSwapService.getAlternatives(null).isEmpty());
    }

    // ---- Map conversion with nutrition + price ----
    @Test void alternativesToMapList_includesPriceAndNutrition() {
        List<SwapOption> alts = IngredientSwapService.getAlternatives("salmon");
        var maps = IngredientSwapService.alternativesToMapList(alts);
        assertFalse(maps.isEmpty());
        // At least one alternative should have price data
        boolean hasPrice = maps.stream().anyMatch(m -> m.containsKey("priceCad"));
        assertTrue(hasPrice, "At least one alternative should have price info");
    }

    // ---- Quantity ratio ----
    @Test void alternatives_crossCategoryHasRatio() {
        List<SwapOption> alts = IngredientSwapService.getAlternatives("chicken thighs");
        // Plant protein swaps should have ratio > 1 (use more tofu to match chicken protein)
        var plantSwap = alts.stream().filter(a -> a.name().equals("tofu")).findFirst();
        if (plantSwap.isPresent()) {
            assertTrue(plantSwap.get().quantityRatio() > 1.0,
                    "Plant protein should have higher quantity ratio");
        }
    }

    @Test void alternatives_sameCategoryHas1to1Ratio() {
        List<SwapOption> alts = IngredientSwapService.getAlternatives("broccoli");
        var cauliflower = alts.stream().filter(a -> a.name().equals("cauliflower")).findFirst();
        assertTrue(cauliflower.isPresent());
        assertEquals(1.0, cauliflower.get().quantityRatio(), "Same-category swaps should be 1:1");
    }
}
