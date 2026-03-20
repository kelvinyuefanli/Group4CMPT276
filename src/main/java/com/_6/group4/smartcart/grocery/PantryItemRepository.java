package com._6.group4.smartcart.grocery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {
    List<PantryItem> findAllByUserIdOrderByIngredientName(Long userId);
    Optional<PantryItem> findFirstByUserIdAndCanonicalNameAndUnitAndQuantityIsNotNull(Long userId, String canonicalName, String unit);
    Optional<PantryItem> findFirstByUserIdAndCanonicalNameAndUnitIsNullAndQuantityIsNotNull(Long userId, String canonicalName);
    void deleteAllByUserId(Long userId);
    void deleteAllByUserIdAndCanonicalName(Long userId, String canonicalName);
    void deleteAllByUserIdAndCanonicalNameAndQuantityIsNull(Long userId, String canonicalName);
    void deleteAllByUserIdAndCanonicalNameAndUnitAndQuantityIsNotNull(Long userId, String canonicalName, String unit);
    void deleteAllByUserIdAndCanonicalNameAndUnitIsNullAndQuantityIsNotNull(Long userId, String canonicalName);
}
