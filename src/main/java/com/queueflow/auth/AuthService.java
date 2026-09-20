package com.queueflow.auth;

import com.queueflow.auth.dto.RegisterRequest;
import com.queueflow.auth.dto.RegisterResponse;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse registerCustomer(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(
                    "An account with this email already exists"
            );
        }

        User user = new User();

        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        // Public registration can only create CUSTOMER accounts.
        user.setRole(UserRole.CUSTOMER);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // Protect against a concurrent registration using the same email.
            throw new ConflictException(
                    "An account with this email already exists"
            );
        }

        return new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole()
        );
    }
}