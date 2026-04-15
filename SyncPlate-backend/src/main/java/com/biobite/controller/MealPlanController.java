package com.biobite.controller;

import com.biobite.config.JwtUtil;
import com.biobite.model.HealthReport;
import com.biobite.model.MealPlan; 
import com.biobite.model.User;
import com.biobite.repository.HealthReportRepository;
import com.biobite.repository.MealPlanRepository; 
import com.biobite.repository.UserRepository;
import com.biobite.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime; 
import java.util.List;
import java.util.Map;
import java.util.Optional; 

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class MealPlanController {

    @Autowired private GeminiService geminiService;
    @Autowired private UserRepository userRepository;
    @Autowired private HealthReportRepository healthReportRepository;
    @Autowired private MealPlanRepository mealPlanRepository; 
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/meal-plan/{userId}")
    public ResponseEntity<?> generateMealPlan(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Verify the authenticated user matches the requested userId
            String token = authHeader.replace("Bearer ", "");
            Long authUserId = jwtUtil.extractUserId(token);
            if (!authUserId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            StringBuilder prompt = new StringBuilder();
            prompt.append("You are Syncplate, an expert AI nutritionist. ");
            prompt.append("Generate a personalized 7-day meal plan for this user.\n\n");
            prompt.append("USER PROFILE:\n");
            prompt.append("Name: ").append(user.getName()).append("\n");
            if (user.getDietaryPreference() != null) prompt.append("Diet: ").append(user.getDietaryPreference()).append("\n");
            if (user.getHealthGoal() != null) prompt.append("Health Goal: ").append(user.getHealthGoal()).append("\n");
            if (user.getCuisinePreference() != null) prompt.append("Cuisine Preference: ").append(user.getCuisinePreference()).append("\n");
            if (user.getHealthConditions() != null && !user.getHealthConditions().isEmpty()) {
                prompt.append("Health Conditions: ");
                user.getHealthConditions().forEach(hc -> prompt.append(hc.getConditionName()).append(", "));
                prompt.append("\n");
            }
            if (user.getAllergies() != null && !user.getAllergies().isEmpty()) {
                prompt.append("ALLERGIES (never include): ");
                user.getAllergies().forEach(a -> prompt.append(a.getAllergen()).append(", "));
                prompt.append("\n");
            }
            if (user.getFoodsToAvoid() != null && !user.getFoodsToAvoid().isEmpty()) {
                prompt.append("Foods to Avoid: ");
                user.getFoodsToAvoid().forEach(f -> prompt.append(f.getFoodName()).append(", "));
                prompt.append("\n");
            }

            // Include health reports
            List<HealthReport> reports = healthReportRepository.findByUserIdOrderByUploadedAtDesc(userId);
            if (!reports.isEmpty()) {
                prompt.append("\nHEALTH REPORTS (use these to personalize the meal plan):\n");
                reports.stream().limit(3).forEach(r -> {
                    prompt.append("- ").append(r.getReportName()).append(": ").append(r.getExtractedValues()).append("\n");
                });
            }

            prompt.append("""

RESPOND ONLY WITH THIS JSON (no markdown, no explanation outside JSON):
{
  "days": [
    {
      "day": "Monday",
      "meals": {
        "breakfast": { "name": "meal name", "description": "brief description", "calories": "~300 kcal", "highlights": ["high protein", "low GI"] },
        "lunch":     { "name": "meal name", "description": "brief description", "calories": "~450 kcal", "highlights": ["rich in fiber"] },
        "dinner":    { "name": "meal name", "description": "brief description", "calories": "~400 kcal", "highlights": ["anti-inflammatory"] },
        "snack":     { "name": "meal name", "description": "brief description", "calories": "~150 kcal", "highlights": ["quick energy"] }
      },
      "totalCalories": "~1300 kcal",
      "nutritionSummary": { "protein": "65g", "carbs": "160g", "fat": "42g", "fiber": "28g" }
    }
  ],
  "shoppingList": {
    "Vegetables & Fruits": ["item1", "item2"],
    "Grains & Legumes": ["item1", "item2"],
    "Dairy & Alternatives": ["item1", "item2"],
    "Proteins": ["item1", "item2"],
    "Pantry & Spices": ["item1", "item2"]
  }
}
""");

            String response = geminiService.getPersonalizedRecommendation(user, prompt.toString());
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/planner/{userId}")
    public ResponseEntity<?> getSavedPlan(@PathVariable Long userId) {
        Optional<MealPlan> plan = mealPlanRepository.findByUserId(userId);
        
        if (plan.isPresent() && plan.get().getPlanData() != null) {
            return ResponseEntity.ok(Map.of("hasPlan", true, "planData", plan.get().getPlanData()));
        } else {
            return ResponseEntity.ok(Map.of("hasPlan", false));
        }
    }

    @PostMapping("/planner/{userId}")
    public ResponseEntity<?> savePlan(@PathVariable Long userId, @RequestBody Map<String, String> payload) {
        // Find existing plan to overwrite, or create a new one to prevent duplicates
        MealPlan plan = mealPlanRepository.findByUserId(userId).orElse(new MealPlan());
        
        plan.setUserId(userId);
        plan.setPlanData(payload.get("planData"));
        plan.setLastUpdated(LocalDateTime.now());
        
        mealPlanRepository.save(plan);
        return ResponseEntity.ok(Map.of("message", "Weekly plan synchronized with database!"));
    }
}
