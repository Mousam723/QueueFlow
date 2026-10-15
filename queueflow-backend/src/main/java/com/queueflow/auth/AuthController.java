package com.queueflow.auth;

import com.queueflow.auth.dto.RegisterRequest;
import com.queueflow.auth.dto.RegisterResponse;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthService authService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response =
                authService.registerCustomer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow();

        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        LoginResponse response = new LoginResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return ResponseEntity.ok(response);
    }
}