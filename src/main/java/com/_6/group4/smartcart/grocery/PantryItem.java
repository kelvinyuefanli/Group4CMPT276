package com._6.group4.smartcart.grocery;

import com._6.group4.smartcart.auth.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pantry_items")
public class PantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column(name = "canonical_name")
    private String canonicalName;

    @Column
    private Double quantity;

    @Column(length = 50)
    private String unit;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PantryItem() {}

    public PantryItem(User user, String ingredientName) {
        this.user = user;
        setIngredientName(ingredientName);
    }

    @PrePersist
    void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
        if (ingredientName == null || ingredientName.isBlank()) {
            this.canonicalName = null;
        } else if (this.canonicalName == null || this.canonicalName.isBlank()) {
            this.canonicalName = IngredientNormalizer.canonicalizeName(ingredientName);
        }
    }
    public String getCanonicalName() { return canonicalName; }
    public void setCanonicalName(String canonicalName) { this.canonicalName = canonicalName; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
