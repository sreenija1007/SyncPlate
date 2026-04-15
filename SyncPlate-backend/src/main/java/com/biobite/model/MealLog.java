package com.biobite.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_logs")
public class MealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String mealName;
    private Boolean isHealthierVersion;
    private LocalDateTime loggedAt;

    // Default constructor
    public MealLog() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; } 

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }

    public Boolean getIsHealthierVersion() { return isHealthierVersion; }
    public void setIsHealthierVersion(Boolean isHealthierVersion) { this.isHealthierVersion = isHealthierVersion; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}