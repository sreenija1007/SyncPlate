package com.biobite.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_plans")
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long userId;

    // We use TEXT here because the Gemini JSON response is huge
    @Column(columnDefinition = "TEXT")
    private String planData; 

    private LocalDateTime lastUpdated;

    // Default Constructor
    public MealPlan() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPlanData() { return planData; }
    public void setPlanData(String planData) { this.planData = planData; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}