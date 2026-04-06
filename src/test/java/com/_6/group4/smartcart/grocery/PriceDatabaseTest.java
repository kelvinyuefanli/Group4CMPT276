package com._6.group4.smartcart.grocery;

import com._6.group4.smartcart.grocery.PriceDatabase.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PriceDatabaseTest {

    // ---- Lookup ----
    @Test void lookup_exactMatch() {
        StorePrice p = PriceDatabase.lookup("chicken thighs");
        assertNotNull(p);
        assertEquals(4.49, p.priceCad());
        assertEquals("lb", p.perUnit());
    }

    @Test void lookup_caseInsensitive() {
        assertNotNull(PriceDatabase.lookup("SALMON"));
    }

    @Test void lookup_fuzzyMatch() {
        StorePrice p = PriceDatabase.lookup("boneless skinless chicken thighs");
        assertNotNull(p);
        assertEquals(4.49, p.priceCad());
    }

    @Test void lookup_unknown() {
        assertNull(PriceDatabase.lookup("zzz unknown item"));
    }

    @Test void lookup_null() {
        assertNull(PriceDatabase.lookup(null));
    }

    // ---- Price estimates ----
    @Test void estimateItemCost_singleUnit() {
        // 1 head of broccoli = $2.99
        double cost = PriceDatabase.estimateItemCost("broccoli", "1 head");
        assertEquals(2.99, cost, 0.01);
    }

    @Test void estimateItemCost_multipleUnits() {
        // 3 lbs ground beef = $5.99 * 3 = $17.97
        double cost = PriceDatabase.estimateItemCost("ground beef", "3 lbs");
        assertEquals(17.97, cost, 0.01);
    }

    @Test void estimateItemCost_dozen() {
        // 2 dozen eggs = $4.49 * 2 = $8.98
        double cost = PriceDatabase.estimateItemCost("eggs", "2 dozen");
        assertEquals(8.98, cost, 0.01);
    }

    @Test void estimateItemCost_halfDozen() {
        // ½ dozen eggs = $4.49 * 1 = $4.49
        double cost = PriceDatabase.estimateItemCost("eggs", "½ dozen");
        assertEquals(4.49, cost, 0.01);
    }

    @Test void estimateItemCost_unknownItem() {
        assertEquals(0, PriceDatabase.estimateItemCost("zzz unknown", "1 bag"));
    }

    // ---- parseStoreQuantityCount ----
    @Test void parseCount_normal() {
        assertEquals(3, PriceDatabase.parseStoreQuantityCount("3 lbs"));
    }

    @Test void parseCount_half() {
        assertEquals(1, PriceDatabase.parseStoreQuantityCount("½ dozen"));
    }

    @Test void parseCount_null() {
        assertEquals(1, PriceDatabase.parseStoreQuantityCount(null));
    }

    @Test void parseCount_noNumber() {
        assertEquals(1, PriceDatabase.parseStoreQuantityCount("some text"));
    }

    // ---- Weekly budget ----
    @Test void estimateWeeklyCost_basicList() {
        List<GroceryItemInput> items = List.of(
                new GroceryItemInput("chicken thighs", "3 lbs"),
                new GroceryItemInput("broccoli", "2 heads"),
                new GroceryItemInput("brown rice", "1 bag"),
                new GroceryItemInput("eggs", "1 dozen")
        );
        WeeklyBudgetEstimate est = PriceDatabase.estimateWeeklyCost(items, 2);
        assertTrue(est.totalCost() > 20); // chicken $13.47 + broccoli $5.98 + rice $5.49 + eggs $4.49 = ~$29
        assertTrue(est.totalCost() < 40);
        assertEquals(80.0, est.recommendedMinimum()); // 2 people * $40
        assertEquals("under_budget", est.budgetStatus());
    }

    @Test void estimateWeeklyCost_sortsByExpensive() {
        List<GroceryItemInput> items = List.of(
                new GroceryItemInput("eggs", "1 dozen"),
                new GroceryItemInput("salmon", "2 lbs"),
                new GroceryItemInput("broccoli", "1 head")
        );
        WeeklyBudgetEstimate est = PriceDatabase.estimateWeeklyCost(items, 1);
        // Salmon should be first (most expensive)
        assertEquals("salmon", est.items().get(0).name());
    }

    @Test void estimateWeeklyCost_emptyList() {
        WeeklyBudgetEstimate est = PriceDatabase.estimateWeeklyCost(List.of(), 2);
        assertEquals(0, est.totalCost());
        assertTrue(est.items().isEmpty());
    }

    // ---- Realistic price checks (sanity) ----
    @Test void prices_meatIsReasonable() {
        // Chicken thighs should be $3-7/lb in Canada
        StorePrice p = PriceDatabase.lookup("chicken thighs");
        assertTrue(p.priceCad() >= 3.0 && p.priceCad() <= 7.0,
                "Chicken thighs price " + p.priceCad() + " outside reasonable range");
    }

    @Test void prices_salmonIsExpensive() {
        StorePrice salmon = PriceDatabase.lookup("salmon");
        StorePrice chicken = PriceDatabase.lookup("chicken thighs");
        assertTrue(salmon.priceCad() > chicken.priceCad(),
                "Salmon should be more expensive than chicken thighs");
    }

    @Test void prices_eggsReasonable() {
        StorePrice p = PriceDatabase.lookup("eggs");
        assertTrue(p.priceCad() >= 3.0 && p.priceCad() <= 8.0,
                "Eggs price " + p.priceCad() + " outside reasonable range");
    }

    @Test void prices_produceReasonable() {
        StorePrice p = PriceDatabase.lookup("bananas");
        assertTrue(p.priceCad() >= 0.15 && p.priceCad() <= 0.50,
                "Banana price " + p.priceCad() + " outside reasonable range per banana");
    }
}
