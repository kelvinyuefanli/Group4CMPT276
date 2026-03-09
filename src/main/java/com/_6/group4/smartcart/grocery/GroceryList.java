package com._6.group4.smartcart.grocery;

import com._6.group4.smartcart.auth.User;
import com._6.group4.smartcart.mealplanning.MealPlan;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grocery_lists")
public class GroceryList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id")
    private MealPlan mealPlan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "groceryList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroceryListItem> items = new ArrayList<>();

    protected GroceryList() {}

    public GroceryList(User user) {
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public MealPlan getMealPlan() { return mealPlan; }
    public void setMealPlan(MealPlan mealPlan) { this.mealPlan = mealPlan; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<GroceryListItem> getItems() { return items; }
}
