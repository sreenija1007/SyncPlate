package com.biobite.controller;

import com.biobite.config.JwtUtil;
import com.biobite.model.User;
import com.biobite.repository.UserRepository;
import com.biobite.service.EmailService;
import com.biobite.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "An account with this email already exists. Please log in instead."
                ));
            }
            User user = new User();
            user.setName((String) request.get("name"));
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode((String) request.get("password")));
            user.setEmailVerified(false);
            user.setProfileCompleted(false);
            String token = UUID.randomUUID().toString();
            user.setVerificationToken(token);
            User saved = userService.registerUser(
                user, List.of(), List.of(), List.of(), List.of()
            );
            emailService.sendVerificationEmail(saved.getEmail(), saved.getName(), token);
            return ResponseEntity.ok(Map.of(
                "message", "Registration successful! Please check your email to verify your account.",
                "email", saved.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        return userRepository.findByVerificationToken(token)
            .map(user -> {
                user.setEmailVerified(true);
                user.setVerificationToken(null);
                userRepository.save(user);

                String jwtToken = jwtUtil.generateToken(user.getEmail(), user.getId());
                return ResponseEntity.ok(Map.of(
                    "token", jwtToken,
                    "userId", user.getId(),
                    "name", user.getName(),
                    "message", "Email verified successfully!"
                ));
            })
            .orElse(ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid or expired verification link."
            )));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email    = request.get("email");
        String password = request.get("password");

        return userService.getUserByEmail(email)
            .map(user -> {
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid email or password"
                    ));
                }
                if (!user.getEmailVerified()) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Please verify your email before logging in. Check your inbox.",
                        "unverified", true
                    ));
                }
                String token = jwtUtil.generateToken(user.getEmail(), user.getId());
                return ResponseEntity.ok(Map.of(
                    "token", token,
                    "userId", user.getId(),
                    "name", user.getName(),
                    "message", "Login successful!", "profileCompleted", user.getProfileCompleted()
                ));
            })
            .orElse(ResponseEntity.badRequest().body(Map.of(
                "error", "No account found with this email. Please register first."
            )));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId  = jwtUtil.extractUserId(token);
        return userService.getUserById(userId)
            .map(user -> ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "city", user.getCity() != null ? user.getCity() : "",
                "healthConditions", user.getHealthConditions(),
                "foodPreferences", user.getFoodPreferences(),
                "allergies", user.getAllergies(),
                "foodsToAvoid", user.getFoodsToAvoid()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            user.setDateOfBirth((String) request.get("dateOfBirth"));
            user.setCity((String) request.get("city"));
            user.setDietaryPreference((String) request.get("dietaryPreference"));
            user.setHealthGoal((String) request.get("healthGoal"));
            user.setCookingStyle((String) request.get("cookingStyle"));
            user.setCuisinePreference((String) request.get("cuisinePreference"));
            user.setProfileCompleted(true);

            List<String> healthConditions = (List<String>) request.get("healthConditions");
            List<String> foodPreferences  = (List<String>) request.get("foodPreferences");
            List<String> allergies        = (List<String>) request.get("allergies");
            List<String> foodsToAvoid     = (List<String>) request.get("foodsToAvoid");

            userService.updateUserProfile(
                user, healthConditions, foodPreferences, allergies, foodsToAvoid
            );

            return ResponseEntity.ok(Map.of("message", "Profile completed!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> profile = new java.util.HashMap<>();
            profile.put("id", user.getId());
            profile.put("name", user.getName());
            profile.put("email", user.getEmail());
            profile.put("dateOfBirth", user.getDateOfBirth());
            profile.put("city", user.getCity());
            profile.put("dietaryPreference", user.getDietaryPreference());
            profile.put("healthGoal", user.getHealthGoal());
            profile.put("cookingStyle", user.getCookingStyle());
            profile.put("cuisinePreference", user.getCuisinePreference());
            profile.put("profileCompleted", user.getProfileCompleted());
            profile.put("healthConditions", user.getHealthConditions().stream()
                .map(hc -> hc.getConditionName()).collect(java.util.stream.Collectors.toList()));
            profile.put("foodPreferences", user.getFoodPreferences().stream()
                .map(fp -> fp.getPreferenceValue()).collect(java.util.stream.Collectors.toList()));
            profile.put("allergies", user.getAllergies().stream()
                .map(a -> a.getAllergen()).collect(java.util.stream.Collectors.toList()));
            profile.put("foodsToAvoid", user.getFoodsToAvoid().stream()
                .map(f -> f.getFoodName()).collect(java.util.stream.Collectors.toList()));

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            if (request.containsKey("name")) user.setName((String) request.get("name"));
            if (request.containsKey("dateOfBirth")) user.setDateOfBirth((String) request.get("dateOfBirth"));
            if (request.containsKey("city")) user.setCity((String) request.get("city"));
            if (request.containsKey("dietaryPreference")) user.setDietaryPreference((String) request.get("dietaryPreference"));
            if (request.containsKey("healthGoal")) user.setHealthGoal((String) request.get("healthGoal"));
            if (request.containsKey("cookingStyle")) user.setCookingStyle((String) request.get("cookingStyle"));
            if (request.containsKey("cuisinePreference")) user.setCuisinePreference((String) request.get("cuisinePreference"));

            List<String> healthConditions = (List<String>) request.get("healthConditions");
            List<String> foodPreferences  = (List<String>) request.get("foodPreferences");
            List<String> allergies        = (List<String>) request.get("allergies");
            List<String> foodsToAvoid     = (List<String>) request.get("foodsToAvoid");

            userService.updateUserProfile(user, healthConditions, foodPreferences, allergies, foodsToAvoid);
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String currentPassword = (String) request.get("currentPassword");
            String newPassword     = (String) request.get("newPassword");

            if (!passwordEncoder.matches(currentPassword, user.getPassword()))
                return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            userRepository.deleteById(userId);
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
