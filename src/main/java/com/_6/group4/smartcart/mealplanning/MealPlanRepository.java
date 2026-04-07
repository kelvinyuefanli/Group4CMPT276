package com._6.group4.smartcart.mealplanning;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    Optional<MealPlan> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    Page<MealPlan> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<MealPlan> findByIdAndUserId(Long id, Long userId);
}
