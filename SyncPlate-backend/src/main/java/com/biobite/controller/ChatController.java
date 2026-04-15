package com.biobite.controller;

import com.biobite.config.JwtUtil;
import com.biobite.model.Analysis;
import com.biobite.model.HealthReport;
import com.biobite.model.User;
import com.biobite.repository.AnalysisRepository;
import com.biobite.repository.HealthReportRepository;
import com.biobite.repository.UserRepository;
import com.biobite.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired private GeminiService geminiService;
    @Autowired private UserRepository userRepository;
    @Autowired private AnalysisRepository analysisRepository;
    @Autowired private HealthReportRepository healthReportRepository;
    @Autowired private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long getAuthUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }

    @PostMapping("/chat/{userId}")
    public ResponseEntity<?> chat(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            Long authUserId = getAuthUserId(authHeader);
            if (!authUserId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String message = request.get("message");
            String imageBase64 = request.get("image");

            List<HealthReport> reports = healthReportRepository.findByUserIdOrderByUploadedAtDesc(userId);
            StringBuilder enrichedMessage = new StringBuilder(message != null ? message : "");
            if (!reports.isEmpty()) {
                enrichedMessage.append("\n\nUSER'S HEALTH REPORTS (factor these into your analysis):\n");
                reports.stream().limit(3).forEach(r -> {
                    enrichedMessage.append("Report: ").append(r.getReportName()).append("\n");
                    if (r.getExtractedValues() != null)
                        enrichedMessage.append("Values: ").append(r.getExtractedValues()).append("\n");
                });
            }

            String response;
            if (imageBase64 != null && !imageBase64.isBlank()) {
                response = geminiService.getPersonalizedRecommendationWithImage(
                    user, enrichedMessage.toString(), imageBase64
                );
            } else {
                response = geminiService.getPersonalizedRecommendation(
                    user, enrichedMessage.toString()
                );
            }

            try {
                String clean = response.replaceAll("```json|```", "").trim();
                Map parsed = objectMapper.readValue(clean, Map.class);
                Analysis analysis = new Analysis();
                analysis.setUser(user);
                analysis.setInputText(message);
                analysis.setResultJson(response);
                analysis.setMealName((String) parsed.getOrDefault("mealName", message));
                analysis.setOverallVerdict((String) parsed.getOrDefault("overallVerdict", "Unknown"));
                Object score = parsed.get("overallScore");
                if (score instanceof Integer) analysis.setOverallScore((Integer) score);
                else if (score instanceof Number) analysis.setOverallScore(((Number) score).intValue());
                analysisRepository.save(analysis);
            } catch (Exception e) {
                System.err.println("Could not save analysis: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/analyses/{userId}")
    public ResponseEntity<?> getAnalyses(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long authUserId = getAuthUserId(authHeader);
            if (!authUserId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            List<Analysis> analyses = analysisRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(analyses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/analyses/{analysisId}")
    public ResponseEntity<?> deleteAnalysis(
            @PathVariable Long analysisId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long authUserId = getAuthUserId(authHeader);
            Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Analysis not found"));
            if (!analysis.getUser().getId().equals(authUserId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            analysisRepository.deleteById(analysisId);
            return ResponseEntity.ok(Map.of("message", "Analysis deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/followup/{userId}")
    public ResponseEntity<?> followUp(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            Long authUserId = getAuthUserId(authHeader);
            if (!authUserId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String question = request.get("question");
            String analysisContext = request.get("context");

            String response = geminiService.getFollowUpResponse(user, question, analysisContext);
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/suggest-recipes/{userId}")
    public ResponseEntity<?> suggestRecipes(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            Long authUserId = getAuthUserId(authHeader);
            if (!authUserId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            List<String> ingredients = (List<String>) request.get("ingredients");
            String mealType = (String) request.getOrDefault("mealType", "");

            String response = geminiService.getRecipeSuggestions(user, ingredients, mealType);
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/recipe-steps/{userId}")
    public ResponseEntity<?> getRecipeSteps(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            Long authUserId = getAuthUserId(authHeader);
            if (!authUserId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String recipeName = (String) request.get("recipeName");
            List<String> ingredients = (List<String>) request.get("ingredients");

            String response = geminiService.getRecipeSteps(user, recipeName, ingredients);
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}