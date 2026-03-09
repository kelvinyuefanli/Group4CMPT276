package com._6.group4.smartcart.grocery;

import jakarta.persistence.*;

@Entity
@Table(name = "grocery_list_items")
public class GroceryListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grocery_list_id", nullable = false)
    private GroceryList groceryList;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column
    private Double quantity;

    @Column(length = 50)
    private String unit;

    @Column(nullable = false)
    private boolean checked = false;

    protected GroceryListItem() {}

    public GroceryListItem(GroceryList groceryList, String ingredientName) {
        this.groceryList = groceryList;
        this.ingredientName = ingredientName;
    }

    public Long getId() { return id; }
    public GroceryList getGroceryList() { return groceryList; }
    public void setGroceryList(GroceryList groceryList) { this.groceryList = groceryList; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
}
