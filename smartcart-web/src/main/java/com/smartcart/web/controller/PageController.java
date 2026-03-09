package com.smartcart.web.controller;

import com.smartcart.web.service.GeminiService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
public class PageController {

    private final GeminiService geminiService;

    public PageController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> index() {
        String html = generateHtmlPage(null, null, null);
        return ResponseEntity.ok(html);
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> generatePlan(
        @RequestParam(defaultValue = "") String pantryIngredients,
        @RequestParam(defaultValue = "None") String preferences
    ) {
        String planHtml = geminiService.generateMealPlan(pantryIngredients, preferences);
        String html = generateHtmlPage("planner", pantryIngredients + "|" + preferences, planHtml);
        return ResponseEntity.ok(html);
    }

    @PostMapping(value = "/generate-recipe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> generateRecipe(@RequestParam(defaultValue = "") String ingredients,
                                                   @RequestParam(defaultValue = "") String cuisine) {
        String recipeHtml = geminiService.generateRecipe(ingredients, cuisine);
        String html = generateHtmlPage("recipe", ingredients + "|" + cuisine, recipeHtml);
        return ResponseEntity.ok(html);
    }

    private String generateHtmlPage(String activeTab, String inputData, String generatedContent) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<title>SmartCart AI</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 40px; }");
        html.append(".container { max-width: 1000px; margin: 0 auto; background-color: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); padding: 30px; }");
        html.append("h1 { color: #059669; margin-bottom: 20px; }");
        html.append(".tab-buttons { display: flex; gap: 15px; border-bottom: 2px solid #e5e7eb; margin-bottom: 20px; }");
        html.append(".tab-button { padding: 10px 20px; font-weight: bold; border: none; background: none; cursor: pointer; color: #6b7280; border-bottom: 2px solid transparent; margin-bottom: -2px; }");
        html.append(".tab-button.active { color: #059669; border-bottom-color: #059669; }");
        html.append(".tab-content { display: none; }");
        html.append(".tab-content.active { display: block; }");
        html.append("form { margin-bottom: 30px; }");
        html.append("label { display: block; font-weight: bold; color: #374151; margin-bottom: 8px; }");
        html.append("input[type='text'] { width: 100%; padding: 10px; border: 1px solid #d1d5db; border-radius: 4px; font-size: 14px; box-sizing: border-box; }");
        html.append(".input-group { display: flex; gap: 10px; margin-bottom: 15px; }");
        html.append("button[type='submit'] { background-color: #059669; color: white; padding: 10px 24px; border: none; border-radius: 4px; font-weight: bold; cursor: pointer; }");
        html.append("button[type='submit']:hover { background-color: #047857; }");
        html.append("h2 { font-size: 20px; font-weight: bold; margin-bottom: 15px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
        html.append("th { background-color: #f3f4f6; font-weight: bold; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<h1>SmartCart AI</h1>");
        
        // Tab buttons
        html.append("<div class='tab-buttons'>");
        html.append("<button onclick=\"showTab('planner')\" id=\"tab-planner\" class='tab-button ");
        html.append(activeTab == null || activeTab.equals("planner") ? "active" : "");
        html.append("'>Meal Planner</button>");
        html.append("<button onclick=\"showTab('recipe')\" id=\"tab-recipe\" class='tab-button ");
        html.append(activeTab != null && activeTab.equals("recipe") ? "active" : "");
        html.append("'>Recipe Generator</button>");
        html.append("</div>");
        
        // Meal Planner Tab
        html.append("<div id='content-planner' class='tab-content ");
        html.append(activeTab == null || activeTab.equals("planner") ? "active" : "");
        html.append("'>");

        String plannerIngredients = "";
        String plannerPreferences = "";
        if (activeTab != null && activeTab.equals("planner") && inputData != null) {
            String[] plannerParts = inputData.split("\\|", 2);
            plannerIngredients = plannerParts[0];
            plannerPreferences = plannerParts.length > 1 ? plannerParts[1] : "";
        }
        
        html.append("<form action='/generate' method='post'>");
        html.append("<label>Ingredients You Have (comma separated)</label>");
        html.append("<input type='text' name='pantryIngredients' value='");
        html.append(escapeHtml(plannerIngredients));
        html.append("' placeholder='e.g. eggs, rice, chicken breast, tomatoes'>");
        html.append("<label>Dietary Preferences / Constraints</label>");
        html.append("<div class='input-group'>");
        html.append("<input type='text' name='preferences' value='");
        html.append(escapeHtml(plannerPreferences));
        html.append("' placeholder='e.g. Vegan, Keto, High Protein' style='flex: 1;'>");
        html.append("<button type='submit'>Generate Plan + Recipes</button>");
        html.append("</div>");
        html.append("</form>");
        
        if (activeTab != null && activeTab.equals("planner") && generatedContent != null) {
            html.append("<div>");
            html.append("<h2>Your Weekly Plan and Detailed Recipes</h2>");
            html.append("<div>");
            html.append(generatedContent);
            html.append("</div>");
            html.append("</div>");
        }
        html.append("</div>");
        
        // Recipe Generator Tab
        html.append("<div id='content-recipe' class='tab-content ");
        html.append(activeTab != null && activeTab.equals("recipe") ? "active" : "");
        html.append("'>");
        html.append("<form action='/generate-recipe' method='post'>");
        html.append("<label>Ingredients (comma separated)</label>");
        html.append("<input type='text' name='ingredients' value='");
        String ingredients = "";
        String cuisine = "";
        if (activeTab != null && activeTab.equals("recipe") && inputData != null && inputData.contains("|")) {
            String[] parts = inputData.split("\\|", 2);
            ingredients = parts[0];
            cuisine = parts.length > 1 ? parts[1] : "";
        }
        html.append(escapeHtml(ingredients));
        html.append("' placeholder='e.g. chicken, tomatoes, garlic, basil'>");
        html.append("<label>Cuisine Type (Optional)</label>");
        html.append("<div class='input-group'>");
        html.append("<input type='text' name='cuisine' value='");
        html.append(escapeHtml(cuisine));
        html.append("' placeholder='e.g. Italian, Mexican, Asian' style='flex: 1;'>");
        html.append("<button type='submit'>Generate Recipe</button>");
        html.append("</div>");
        html.append("</form>");
        
        if (activeTab != null && activeTab.equals("recipe") && generatedContent != null) {
            html.append("<div>");
            html.append("<h2>Your Recipe</h2>");
            html.append("<div>");
            html.append(generatedContent);
            html.append("</div>");
            html.append("</div>");
        }
        html.append("</div>");
        
        // JavaScript for tab switching
        html.append("<script>");
        html.append("function showTab(tabName) {");
        html.append("  document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));");
        html.append("  document.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));");
        html.append("  document.getElementById('content-' + tabName).classList.add('active');");
        html.append("  document.getElementById('tab-' + tabName).classList.add('active');");
        html.append("}");
        html.append("</script>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
