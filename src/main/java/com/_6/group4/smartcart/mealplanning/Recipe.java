package com._6.group4.smartcart.mealplanning;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "cook_time_minutes")
    private Integer cookTimeMinutes;

    @Column
    private Integer servings;

    @Column(length = 100)
    private String cuisine;

    @Column(length = 50)
    private String source = "gemini";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    protected Recipe() {}

    public Recipe(String title) {
        this.title = title;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public Integer getCookTimeMinutes() { return cookTimeMinutes; }
    public void setCookTimeMinutes(Integer cookTimeMinutes) { this.cookTimeMinutes = cookTimeMinutes; }
    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
}
