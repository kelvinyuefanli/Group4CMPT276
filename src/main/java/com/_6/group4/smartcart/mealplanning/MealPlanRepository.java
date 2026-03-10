package com._6.group4.smartcart.mealplanning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    Optional<MealPlan> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
