package com.biobite.repository;

import com.biobite.model.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    // This allows us to instantly find a user's saved weekly plan
    Optional<MealPlan> findByUserId(Long userId);
}