package com.orderprocessing.order_system.controller;

import com.orderprocessing.order_system.User;
import com.orderprocessing.order_system.repository.UserRepository;
import com.orderprocessing.order_system.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");
        String role = request.getOrDefault("role", "USER");

        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Username already exists");
        }

        // Save user
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User not found");
        }

        User user = userOpt.get();

        if (!user.getPassword().equals(password)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Wrong password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(username, user.getRole());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", username,
                "role", user.getRole()
        ));
    }
}