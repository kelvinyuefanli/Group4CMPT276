package com._6.group4.smartcart.mealplanning;

import jakarta.persistence.*;

@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column
    private Double quantity;

    @Column(length = 50)
    private String unit;

    @Column(name = "canonical_name")
    private String canonicalName;

    protected RecipeIngredient() {}

    public RecipeIngredient(Recipe recipe, String ingredientName) {
        this.recipe = recipe;
        this.ingredientName = ingredientName;
    }

    public Long getId() { return id; }
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getCanonicalName() { return canonicalName; }
    public void setCanonicalName(String canonicalName) { this.canonicalName = canonicalName; }
}
