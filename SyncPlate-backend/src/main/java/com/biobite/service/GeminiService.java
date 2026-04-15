package com.biobite.service;

import com.biobite.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=";

    private static final String ANALYSIS_PROMPT_SUFFIX = """
RESPOND ONLY WITH THIS JSON (no markdown, no explanation outside JSON):
{
  "mealName": "name of the meal/food",
  "overallVerdict": "Safe" | "Caution" | "Avoid",
  "verdictReason": "one sentence summary of why",
  "overallScore": 0-100,
  "scores": {
    "glycemicLoad": { "value": 0-10, "label": "Low/Medium/High", "note": "brief note" },
    "inflammation": { "value": 0-10, "label": "Low/Medium/High", "note": "brief note" },
    "fiber": { "value": 0-10, "label": "Low/Medium/High", "note": "brief note" },
    "nutrients": { "value": 0-10, "label": "Poor/Fair/Good/Excellent", "note": "brief note" }
  },
  "ingredients": [
    { "name": "ingredient name", "status": "Safe" | "Caution" | "Avoid", "reason": "why" }
  ],
  "healthFlags": [
    { "condition": "health condition name", "impact": "Positive" | "Negative" | "Neutral", "explanation": "brief explanation" }
  ],
  "nutritionEstimate": {
    "calories": "estimated range e.g. 350-400 kcal",
    "protein": "e.g. 12g",
    "carbs": "e.g. 45g",
    "fat": "e.g. 8g",
    "fiber": "e.g. 6g"
  },
  "safeSwaps": [
    { "original": "ingredient to swap", "swap": "healthier alternative", "benefit": "why it's better" }
  ],
  "tips": ["tip 1", "tip 2", "tip 3"],
  "betterAlternatives": [
    { "meal": "alternative meal name", "reason": "why it's better for this person" }
  ]
}
""";


    private String cleanJsonResponse(String rawResponse) {
        if (rawResponse == null) return "{}";
        
        // Find the first { and the last }
        int startIndex = rawResponse.indexOf('{');
        int endIndex = rawResponse.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return rawResponse.substring(startIndex, endIndex + 1);
        }
        
        return "{}"; 
    }
    private String buildProfileContext(User user) {
        StringBuilder profile = new StringBuilder();
        profile.append("You are Syncplate, an expert AI health food analyst. ");
        profile.append("Analyze the food/meal provided and return a detailed health analysis.\n\n");

        profile.append("USER PROFILE:\n");
        profile.append("Name: ").append(user.getName()).append("\n");
        if (user.getDateOfBirth() != null)
            profile.append("Date of Birth: ").append(user.getDateOfBirth()).append("\n");
        if (user.getCity() != null)
            profile.append("Location: ").append(user.getCity()).append("\n");
        if (user.getDietaryPreference() != null)
            profile.append("Dietary Preference: ").append(user.getDietaryPreference()).append("\n");
        if (user.getHealthGoal() != null)
            profile.append("Health Goals: ").append(user.getHealthGoal()).append("\n");
        if (user.getCuisinePreference() != null)
            profile.append("Cuisine Preference: ").append(user.getCuisinePreference()).append("\n");

        if (user.getHealthConditions() != null && !user.getHealthConditions().isEmpty()) {
            profile.append("Health Conditions: ");
            user.getHealthConditions().forEach(hc ->
                profile.append(hc.getConditionName()).append(", "));
            profile.append("\n");
        }

        if (user.getFoodPreferences() != null && !user.getFoodPreferences().isEmpty()) {
            profile.append("Food Preferences: ");
            user.getFoodPreferences().forEach(fp ->
                profile.append(fp.getPreferenceValue()).append(", "));
            profile.append("\n");
        }

        if (user.getAllergies() != null && !user.getAllergies().isEmpty()) {
            profile.append("ALLERGIES (flag if present): ");
            user.getAllergies().forEach(a ->
                profile.append(a.getAllergen()).append(", "));
            profile.append("\n");
        }

        if (user.getFoodsToAvoid() != null && !user.getFoodsToAvoid().isEmpty()) {
            profile.append("Foods to Avoid: ");
            user.getFoodsToAvoid().forEach(f ->
                profile.append(f.getFoodName()).append(", "));
            profile.append("\n");
        }

        return profile.toString();
    }

    public String getPersonalizedRecommendation(User user, String userMessage) {
        String profileContext = buildProfileContext(user);
        String fullPrompt = profileContext
            + "\nFOOD TO ANALYZE:\n" + userMessage + "\n\n"
            + ANALYSIS_PROMPT_SUFFIX;

        try {
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", fullPrompt)
                    ))
                )
            );

            return callGemini(requestBody);
        } catch (Exception e) {
            System.err.println("GEMINI ERROR: " + e.getMessage());
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public String getPersonalizedRecommendationWithImage(User user, String userMessage, String imageBase64) {
        String profileContext = buildProfileContext(user);

        String textPrompt = profileContext
            + "\nFOOD TO ANALYZE:\n"
            + (userMessage != null && !userMessage.isBlank() ? userMessage + "\n" : "")
            + "Also analyze the food shown in the attached image.\n\n"
            + ANALYSIS_PROMPT_SUFFIX;

        try {
            String cleanBase64 = imageBase64;
            String mimeType = "image/jpeg"; // default

            if (imageBase64.contains(",")) {
                String prefix = imageBase64.substring(0, imageBase64.indexOf(","));
                cleanBase64 = imageBase64.substring(imageBase64.indexOf(",") + 1);

                if (prefix.contains("image/png")) mimeType = "image/png";
                else if (prefix.contains("image/webp")) mimeType = "image/webp";
                else if (prefix.contains("image/gif")) mimeType = "image/gif";
            }

            // Build multimodal request with inline image data
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", textPrompt),
                        Map.of("inlineData", Map.of(
                            "mimeType", mimeType,
                            "data", cleanBase64
                        ))
                    ))
                )
            );

            return callGemini(requestBody);
        } catch (Exception e) {
            System.err.println("GEMINI MULTIMODAL ERROR: " + e.getMessage());
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String callGemini(Map<String, Object> requestBody) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GEMINI_URL + apiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("Gemini status: " + response.statusCode());

        if (response.statusCode() != 200) {
            System.err.println("Gemini error body: " + response.body());
            throw new RuntimeException("Gemini API returned status " + response.statusCode());
        }

        Map responseMap = objectMapper.readValue(response.body(), Map.class);
        List candidates = (List) responseMap.get("candidates");
        Map candidate = (Map) candidates.get(0);
        Map content = (Map) candidate.get("content");
        List parts = (List) content.get("parts");
        Map part = (Map) parts.get(0);
        return (String) part.get("text");
    }

    public String getFollowUpResponse(User user, String question, String analysisContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are Syncplate, a friendly and knowledgeable AI health food advisor.\n\n");

        prompt.append("USER PROFILE:\n");
        prompt.append("Name: ").append(user.getName()).append("\n");
        if (user.getDietaryPreference() != null)
            prompt.append("Diet: ").append(user.getDietaryPreference()).append("\n");
        if (user.getHealthGoal() != null)
            prompt.append("Goals: ").append(user.getHealthGoal()).append("\n");
        if (user.getHealthConditions() != null && !user.getHealthConditions().isEmpty()) {
            prompt.append("Health Conditions: ");
            user.getHealthConditions().forEach(hc -> prompt.append(hc.getConditionName()).append(", "));
            prompt.append("\n");
        }
        if (user.getAllergies() != null && !user.getAllergies().isEmpty()) {
            prompt.append("Allergies: ");
            user.getAllergies().forEach(a -> prompt.append(a.getAllergen()).append(", "));
            prompt.append("\n");
        }

        if (analysisContext != null && !analysisContext.isBlank()) {
            prompt.append("\nPREVIOUS FOOD ANALYSIS (for context):\n");
            String truncated = analysisContext.length() > 2000
                ? analysisContext.substring(0, 2000) + "..."
                : analysisContext;
            prompt.append(truncated).append("\n");
        }

        prompt.append("\nUSER'S FOLLOW-UP QUESTION: ").append(question).append("\n\n");
        prompt.append("INSTRUCTIONS: Respond in plain conversational English. ");
        prompt.append("Keep your answer to 2-5 sentences. Be specific to their health conditions and dietary preferences. ");
        prompt.append("If they ask about swaps, suggest specific alternatives. ");
        prompt.append("If they ask about portions or timing, give practical advice. ");
        prompt.append("Do NOT return JSON. Do NOT use markdown formatting. Just plain helpful text.");

        try {
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt.toString())
                    ))
                )
            );
            return callGemini(requestBody);
        } catch (Exception e) {
            System.err.println("GEMINI FOLLOWUP ERROR: " + e.getMessage());
            return "Sorry, I couldn't process your question right now. Please try again.";
        }
    }

    public String getRecipeSuggestions(User user, List<String> ingredients, String mealType) {
        String profileContext = buildProfileContext(user);

        StringBuilder prompt = new StringBuilder(profileContext);
        prompt.append("\nThe user has these ingredients available: ").append(String.join(", ", ingredients)).append("\n");
        if (mealType != null && !mealType.isBlank()) {
            prompt.append("They want to make: ").append(mealType).append("\n");
        }
        prompt.append("\nSuggest 4-5 dishes they can make with these ingredients. ");
        prompt.append("Prioritize dishes that are healthy for their specific health conditions. ");
        prompt.append("They don't need to use ALL ingredients — just pick the best combinations.\n\n");

        prompt.append("""
RESPOND ONLY WITH THIS JSON (no markdown, no explanation outside JSON):
{
  "recipes": [
    {
      "name": "dish name",
      "description": "1-2 sentence description of the dish",
      "verdict": "Safe" | "Caution" | "Avoid",
      "cookTime": "e.g. 20 mins",
      "difficulty": "Easy" | "Medium" | "Hard",
      "calories": "e.g. ~350 kcal",
      "highlights": ["high protein", "low GI"],
      "usesIngredients": ["ingredient1", "ingredient2"],
      "missingIngredients": ["optional ingredient not in their list"]
    }
  ]
}
""");

        try {
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt.toString()))))
            );
            return callGemini(requestBody);
        } catch (Exception e) {
            System.err.println("GEMINI RECIPE SUGGESTIONS ERROR: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public String getRecipeSteps(User user, String recipeName, List<String> availableIngredients) {
        String profileContext = buildProfileContext(user);

        StringBuilder prompt = new StringBuilder(profileContext);
        prompt.append("\nGenerate a full recipe for: ").append(recipeName).append("\n");
        if (availableIngredients != null && !availableIngredients.isEmpty()) {
            prompt.append("Available ingredients: ").append(String.join(", ", availableIngredients)).append("\n");
        }
        prompt.append("Make the recipe healthy and suitable for their health conditions. ");
        prompt.append("Suggest healthier swaps where appropriate.\n\n");

        prompt.append("""
RESPOND ONLY WITH THIS JSON (no markdown, no explanation outside JSON):
{
  "name": "recipe name",
  "servings": "2-3",
  "prepTime": "10 mins",
  "cookTime": "20 mins",
  "ingredients": [
    "1 cup rice",
    "200g dal (lentils)",
    "1 tbsp ghee or coconut oil (healthier swap)"
  ],
  "steps": [
    "Wash and soak the dal for 15 minutes.",
    "Heat oil in a pan, add cumin seeds and let them splutter.",
    "Add onions and sauté until golden brown.",
    "Add the dal, water, and salt. Cook for 15 minutes.",
    "Garnish with fresh coriander and serve with rice."
  ],
  "tips": [
    "Use coconut oil instead of ghee for a lighter version",
    "Adding turmeric boosts anti-inflammatory benefits"
  ],
  "healthNotes": "This recipe is suitable for your conditions because..."
}
""");

        try {
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt.toString()))))
            );
            return callGemini(requestBody);
        } catch (Exception e) {
            System.err.println("GEMINI RECIPE STEPS ERROR: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
