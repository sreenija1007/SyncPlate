package com.biobite.repository;

import com.biobite.model.HealthReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface HealthReportRepository extends JpaRepository<HealthReport, Long> {
    List<HealthReport> findByUserIdOrderByUploadedAtDesc(Long userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM HealthReport r WHERE r.id = :id AND r.user.id = :userId")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}