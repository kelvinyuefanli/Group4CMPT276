package com._6.group4.smartcart.grocery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IngredientNormalizerTest {

    @Test
    void canonicalizeName_removesPrepNotesAndNormalizesAliases() {
        assertEquals("green onion", IngredientNormalizer.canonicalizeName(" Spring_Onions, chopped (tops only) "));
        assertEquals("chickpea", IngredientNormalizer.canonicalizeName("Garbanzo Beans"));
    }

    @Test
    void canonicalizeName_singularizesSimplePluralsConservatively() {
        assertEquals("red onion", IngredientNormalizer.canonicalizeName("red onions"));
        assertEquals("tomato", IngredientNormalizer.canonicalizeName("tomatoes"));
        assertNotEquals(
                IngredientNormalizer.canonicalizeName("red onion"),
                IngredientNormalizer.canonicalizeName("yellow onion")
        );
    }
}
