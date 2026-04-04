package com._6.group4.smartcart.mealplanning;

import com._6.group4.smartcart.mealplanning.dto.GeminiRecipeDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GeminiService utility methods (no Spring context needed).
 */
class GeminiServiceTest {

    private final GeminiService service = new GeminiService();
    private final ObjectMapper mapper = new ObjectMapper();

    // ---- sanitizeInput ----

    @Test
    void sanitizeInput_nullReturnsNull() {
        assertThat(GeminiService.sanitizeInput(null)).isNull();
    }

    @Test
    void sanitizeInput_stripsControlCharacters() {
        // \u0000 is a control char, should be removed (not replaced with space)
        String result = GeminiService.sanitizeInput("hello\u0000world");
        assertThat(result).doesNotContain("\u0000");
        assertThat(result).isEqualTo("helloworld");
    }

    @Test
    void sanitizeInput_preservesNormalWhitespace() {
        assertThat(GeminiService.sanitizeInput("hello world")).isEqualTo("hello world");
        assertThat(GeminiService.sanitizeInput("line1\nline2")).isEqualTo("line1\nline2");
    }

    @Test
    void sanitizeInput_truncatesLongInput() {
        String longInput = "a".repeat(600);
        String result = GeminiService.sanitizeInput(longInput);
        assertThat(result.length()).isEqualTo(500);
    }

    @Test
    void sanitizeInput_stripsLeadingTrailingWhitespace() {
        assertThat(GeminiService.sanitizeInput("  hello  ")).isEqualTo("hello");
    }

    // ---- parseJson ----

    @Test
    void parseJson_nullReturnsNull() {
        assertThat(service.parseJson(null, GeminiRecipeDto.class)).isNull();
    }

    @Test
    void parseJson_emptyStringReturnsNull() {
        assertThat(service.parseJson("", GeminiRecipeDto.class)).isNull();
        assertThat(service.parseJson("   ", GeminiRecipeDto.class)).isNull();
    }

    @Test
    void parseJson_invalidJsonReturnsNull() {
        assertThat(service.parseJson("not json at all", GeminiRecipeDto.class)).isNull();
    }

    @Test
    void parseJson_validJsonParsesCorrectly() {
        String json = """
            {"title":"Test Recipe","cuisine":"Italian","cookTimeMinutes":30,
             "servings":4,"instructions":"Step 1","ingredients":[]}""";
        GeminiRecipeDto result = service.parseJson(json, GeminiRecipeDto.class);
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Test Recipe");
        assertThat(result.cuisine()).isEqualTo("Italian");
    }

    // ---- sampleFromList ----

    @Test
    void sampleFromList_nullReturnsNull() {
        assertThat(GeminiService.sampleFromList(null, 3)).isNull();
    }

    @Test
    void sampleFromList_blankReturnsBlank() {
        assertThat(GeminiService.sampleFromList("  ", 3)).isEqualTo("  ");
    }

    @Test
    void sampleFromList_underMaxReturnsAll() {
        String input = "Chicken, Beef";
        assertThat(GeminiService.sampleFromList(input, 3)).isEqualTo(input);
    }

    @Test
    void sampleFromList_exactMaxReturnsAll() {
        String input = "Chicken, Beef, Salmon";
        assertThat(GeminiService.sampleFromList(input, 3)).isEqualTo(input);
    }

    @Test
    void sampleFromList_overMaxSamplesCorrectCount() {
        String input = "Chicken, Beef, Salmon, Turkey, Pork, Shrimp, Tofu, Cod";
        String result = GeminiService.sampleFromList(input, 3);
        String[] items = result.split(",");
        assertThat(items).hasSize(3);
        // Each item should be from the original list
        for (String item : items) {
            assertThat(input).contains(item.trim());
        }
    }

    @Test
    void sampleFromList_overMaxProducesVariety() {
        // Run 20 times — should not always return the same 3
        String input = "A, B, C, D, E, F, G, H";
        java.util.Set<String> allResults = new java.util.HashSet<>();
        for (int i = 0; i < 20; i++) {
            allResults.add(GeminiService.sampleFromList(input, 3));
        }
        // With 8 items choosing 3, we should see at least 2 different combinations
        assertThat(allResults.size()).isGreaterThan(1);
    }

    @Test
    void parseJson_stripsMarkdownFences() {
        String json = """
            ```json
            {"title":"Fenced Recipe","cuisine":null,"cookTimeMinutes":10,
             "servings":2,"instructions":"Cook it","ingredients":[]}
            ```""";
        GeminiRecipeDto result = service.parseJson(json, GeminiRecipeDto.class);
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Fenced Recipe");
    }
}
