package com.biobite.repository;

import com.biobite.model.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HealthConditionRepository extends JpaRepository<HealthCondition, Long> {
    List<HealthCondition> findByUserId(Long userId);
}
