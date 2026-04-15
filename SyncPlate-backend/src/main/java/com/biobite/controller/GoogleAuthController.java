package com.biobite.controller;

import com.biobite.model.User;
import com.biobite.repository.UserRepository;
import com.biobite.config.JwtUtil; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload) {
        try {
            String token = payload.get("token");

            RestTemplate restTemplate = new RestTemplate();
            String googleUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> googleInfo = restTemplate.getForObject(googleUrl, Map.class);

            if (googleInfo == null || !googleInfo.containsKey("email")) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Google token"));
            }

            String email = (String) googleInfo.get("email");
            String name = (String) googleInfo.get("name");

            // Find the user or create a new one!
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name != null ? name : "Google User");
                newUser.setPassword(UUID.randomUUID().toString()); 
                
                newUser.setProfileCompleted(false);
                newUser.setEmailVerified(true); 
                return userRepository.save(newUser);
            });

            // Generate your Syncplate token
            String biobiteToken = jwtUtil.generateToken(user.getEmail(), user.getId());

            return ResponseEntity.ok(Map.of(
                "token", biobiteToken,
                "profileCompleted", user.getProfileCompleted(),
                "user", Map.of(
                    "userId", user.getId(),
                    "name", user.getName(),
                    "profileCompleted", user.getProfileCompleted()
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Google login failed: " + e.getMessage()));
        }
    }
}
