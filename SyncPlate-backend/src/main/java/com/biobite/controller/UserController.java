package com.biobite.controller;

import com.biobite.config.JwtUtil;
import com.biobite.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Only allow users to look up their own data
            String token = authHeader.replace("Bearer ", "");
            Long authUserId = jwtUtil.extractUserId(token);
            if (!authUserId.equals(id)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
