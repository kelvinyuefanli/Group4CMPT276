package com._6.group4.smartcart.grocery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {
    List<PantryItem> findAllByUserIdOrderByIngredientName(Long userId);
    void deleteAllByUserId(Long userId);
}
