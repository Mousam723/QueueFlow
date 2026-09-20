package com.queueflow.auth;

import com.queueflow.auth.dto.RegisterRequest;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "queueflow.security.enabled=true")
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterCustomer() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "StrongPassword123",
                "9876543210"
        );

        String response = mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("9876543210"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson =
                objectMapper.readTree(response);

        Long userId =
                responseJson.get("userId").asLong();

        User user = userRepository.findById(userId).orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(
                UserRole.CUSTOMER,
                user.getRole()
        );

        org.junit.jupiter.api.Assertions.assertTrue(
                passwordEncoder.matches(
                        "StrongPassword123",
                        user.getPasswordHash()
                )
        );
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {

        User existingUser = new User();
        existingUser.setName("Existing User");
        existingUser.setEmail("existing@example.com");
        existingUser.setPhone("9999999999");
        existingUser.setPasswordHash(
                passwordEncoder.encode("ExistingPassword123")
        );
        existingUser.setRole(UserRole.CUSTOMER);

        java.time.LocalDateTime now =
                java.time.LocalDateTime.now();

        existingUser.setCreatedAt(now);
        existingUser.setUpdatedAt(now);

        userRepository.saveAndFlush(existingUser);

        RegisterRequest request = new RegisterRequest(
                "Another User",
                "existing@example.com",
                "StrongPassword123",
                "8888888888"
        );

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void shouldRejectInvalidRegistrationRequest() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "",
                "not-an-email",
                "short",
                "123"
        );

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldNotStorePlaintextPassword() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "Password Test",
                "password-test@example.com",
                "StrongPassword123",
                null
        );

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated());

        User user = userRepository
                .findByEmail("password-test@example.com")
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertNotEquals(
                "StrongPassword123",
                user.getPasswordHash()
        );

        org.junit.jupiter.api.Assertions.assertTrue(
                user.getPasswordHash().startsWith("$2")
        );
    }

    @Test
    void shouldCreateCustomerRegardlessOfAuthenticationState() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "Public Customer",
                "public-customer@example.com",
                "StrongPassword123",
                null
        );

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated());

        User user = userRepository
                .findByEmail("public-customer@example.com")
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(
                UserRole.CUSTOMER,
                user.getRole()
        );
    }
}