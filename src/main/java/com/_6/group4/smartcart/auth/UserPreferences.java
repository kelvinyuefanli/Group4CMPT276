package com._6.group4.smartcart.auth;

import jakarta.persistence.*;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "serving_size", nullable = false)
    private int servingSize = 2;

    @Column(name = "dietary_restrictions", length = 1000)
    private String dietaryRestrictions;

    @Column(length = 1000)
    private String allergies;

    @Column(name = "preferred_cuisines", length = 1000)
    private String preferredCuisines;

    @Column(name = "rotate_cuisines", nullable = false)
    private boolean rotateCuisines = true;

    @Column(name = "disliked_foods", length = 1000)
    private String dislikedFoods;

    @Column(name = "meal_schedule", length = 2000)
    private String mealSchedule;

    @Column(name = "preferred_proteins", length = 1000)
    private String preferredProteins;

    @Column(name = "preferred_vegetables", length = 1000)
    private String preferredVegetables;

    @Column(name = "preferred_fruits", length = 1000)
    private String preferredFruits;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    protected UserPreferences() {}

    public UserPreferences(User user) {
        this.user = user;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public int getServingSize() { return servingSize; }
    public void setServingSize(int servingSize) { this.servingSize = servingSize; }
    public String getDietaryRestrictions() { return dietaryRestrictions; }
    public void setDietaryRestrictions(String dietaryRestrictions) { this.dietaryRestrictions = dietaryRestrictions; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getPreferredCuisines() { return preferredCuisines; }
    public void setPreferredCuisines(String preferredCuisines) { this.preferredCuisines = preferredCuisines; }
    public boolean isRotateCuisines() { return rotateCuisines; }
    public void setRotateCuisines(boolean rotateCuisines) { this.rotateCuisines = rotateCuisines; }
    public String getDislikedFoods() { return dislikedFoods; }
    public void setDislikedFoods(String dislikedFoods) { this.dislikedFoods = dislikedFoods; }
    public String getMealSchedule() { return mealSchedule; }
    public void setMealSchedule(String mealSchedule) { this.mealSchedule = mealSchedule; }
    public String getPreferredProteins() { return preferredProteins; }
    public void setPreferredProteins(String preferredProteins) { this.preferredProteins = preferredProteins; }
    public String getPreferredVegetables() { return preferredVegetables; }
    public void setPreferredVegetables(String preferredVegetables) { this.preferredVegetables = preferredVegetables; }
    public String getPreferredFruits() { return preferredFruits; }
    public void setPreferredFruits(String preferredFruits) { this.preferredFruits = preferredFruits; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
}
