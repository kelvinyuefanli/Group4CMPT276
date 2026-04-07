package com._6.group4.smartcart.grocery;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StoreQuantityConverterTest {

    // ---- Eggs ----
    @Test void eggs_smallCount_halfDozen() {
        assertEquals("½ dozen", StoreQuantityConverter.convert("Large Eggs", 4.0, "count"));
    }
    @Test void eggs_dozen() {
        assertEquals("1 dozen", StoreQuantityConverter.convert("Eggs", 12.0, "count"));
    }
    @Test void eggs_overDozen() {
        assertEquals("2 dozen", StoreQuantityConverter.convert("Large Eggs", 16.0, "count"));
    }

    // ---- Meat (grams to lbs) ----
    @Test void groundBeef_gramsToLbs() {
        assertEquals("3 lbs", StoreQuantityConverter.convert("Ground Beef", 1200.0, "g"));
    }
    @Test void salmon_smallGrams() {
        assertEquals("1 lb", StoreQuantityConverter.convert("Salmon Fillet", 200.0, "g"));
    }
    @Test void chickenThighs_count() {
        assertEquals("6 pieces", StoreQuantityConverter.convert("Chicken Thighs", 6.0, "count"));
    }
    @Test void groundTurkey_kg() {
        assertEquals("3 lbs", StoreQuantityConverter.convert("Ground Turkey", 1.0, "kg"));
    }

    // ---- Produce (heads) ----
    @Test void broccoli_cupsToHeads() {
        assertEquals("2 heads", StoreQuantityConverter.convert("Broccoli", 8.0, "cup"));
    }
    @Test void cauliflower_singleHead() {
        assertEquals("1 head", StoreQuantityConverter.convert("Cauliflower", 3.0, "cup"));
    }

    // ---- Leafy greens (bags) ----
    @Test void spinach_cupsToBags() {
        assertEquals("4 bags", StoreQuantityConverter.convert("Fresh Spinach", 24.0, "cup"));
    }
    @Test void kale_smallAmount() {
        assertEquals("1 bag", StoreQuantityConverter.convert("Kale", 4.0, "cup"));
    }

    // ---- Root veggies (bags) ----
    @Test void carrots_countToBag() {
        assertEquals("1 bag (2 lb)", StoreQuantityConverter.convert("Carrots", 8.0, "count"));
    }
    @Test void carrots_manyToBags() {
        assertEquals("2 bags (2 lb each)", StoreQuantityConverter.convert("Carrots", 11.0, "count"));
    }

    // ---- Bell peppers (pass through count) ----
    @Test void bellPeppers_countPassThrough() {
        assertEquals("8 count", StoreQuantityConverter.convert("Bell Peppers", 8.0, "count"));
    }

    // ---- Dry goods ----
    @Test void brownRice_cupsToBag() {
        assertEquals("1 bag", StoreQuantityConverter.convert("Brown Rice (uncooked)", 0.75, "cup"));
    }
    @Test void pasta_gramsToBox() {
        assertEquals("1 box", StoreQuantityConverter.convert("Whole Wheat Pasta", 180.0, "g"));
    }
    @Test void quinoa_cupsToBag() {
        assertEquals("1 bag", StoreQuantityConverter.convert("Quinoa (uncooked)", 0.75, "cup"));
    }
    @Test void oats_largeCups() {
        assertEquals("2 bags", StoreQuantityConverter.convert("Rolled Oats", 5.0, "cup"));
    }

    // ---- Dairy ----
    @Test void yogurt_cupsToTub() {
        assertEquals("1 tub", StoreQuantityConverter.convert("Plain Yogurt", 4.0, "cup"));
    }
    @Test void yogurt_manyTubs() {
        assertEquals("2 tubs", StoreQuantityConverter.convert("Greek Yogurt", 6.0, "cup"));
    }
    @Test void milk_cupsToCarton() {
        assertEquals("1 carton", StoreQuantityConverter.convert("Milk", 2.0, "cup"));
    }
    @Test void milk_tbspStillOneCarton() {
        assertEquals("1 carton", StoreQuantityConverter.convert("Milk", 2.0, "tbsp"));
    }

    // ---- Fallback ----
    @Test void unknownItem_passThrough() {
        assertEquals("3.5 tbsp", StoreQuantityConverter.convert("Random Spice", 3.5, "tbsp"));
    }
    @Test void nullQuantity_emptyUnit() {
        assertEquals("", StoreQuantityConverter.convert("Something", null, ""));
    }
    @Test void bananas_countPassThrough() {
        assertEquals("7 count", StoreQuantityConverter.convert("Bananas", 7.0, "count"));
    }
}
