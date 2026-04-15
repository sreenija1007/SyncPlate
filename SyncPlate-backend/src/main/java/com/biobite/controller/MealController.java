package com.biobite.controller;

import com.biobite.model.MealLog;
import com.biobite.repository.MealLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meals")
public class MealController {

    @Autowired
    private MealLogRepository mealLogRepository;

    @PostMapping("/log/{userId}")
    public ResponseEntity<?> logMeal(@PathVariable Long userId, @RequestBody Map<String, Object> payload) {
        try {
            MealLog log = new MealLog();
            log.setUserId(userId);
            log.setMealName((String) payload.get("mealName"));
            
            Object isHealthierObj = payload.get("isHealthierVersion");
            Boolean isHealthier = false;
            if (isHealthierObj != null) {
                isHealthier = isHealthierObj instanceof Boolean ? (Boolean) isHealthierObj : Boolean.valueOf(isHealthierObj.toString());
            }
            log.setIsHealthierVersion(isHealthier);
            
            log.setLoggedAt(LocalDateTime.now());
            
            mealLogRepository.save(log);
            
            return ResponseEntity.ok(Map.of("message", "Meal logged successfully!", "log", log));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to log meal: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserMeals(@PathVariable Long userId) {
        try {
            List<MealLog> meals = mealLogRepository.findByUserIdOrderByLoggedAtDesc(userId);
            return ResponseEntity.ok(meals);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch meals"));
        }
    }
}
