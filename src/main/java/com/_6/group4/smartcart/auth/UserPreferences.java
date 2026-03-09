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

    @Column(name = "dietary_restrictions")
    private String dietaryRestrictions;

    @Column
    private String allergies;

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
}
