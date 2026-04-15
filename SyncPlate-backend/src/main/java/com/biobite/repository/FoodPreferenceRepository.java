package com.biobite.repository;

import com.biobite.model.FoodPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FoodPreferenceRepository extends JpaRepository<FoodPreference, Long> {
    List<FoodPreference> findByUserId(Long userId);
}
