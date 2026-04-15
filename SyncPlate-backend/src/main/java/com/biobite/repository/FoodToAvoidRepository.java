package com.biobite.repository;

import com.biobite.model.FoodToAvoid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FoodToAvoidRepository extends JpaRepository<FoodToAvoid, Long> {
    List<FoodToAvoid> findByUserId(Long userId);
}
