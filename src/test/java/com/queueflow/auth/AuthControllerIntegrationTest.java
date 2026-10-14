package com.queueflow.auth;

import com.queueflow.auth.dto.RegisterRequest;
import com.queueflow.auth.dto.RegisterResponse;
import com.queueflow.appointment.AppointmentRepository;
import com.queueflow.business.BusinessRepository;
import com.queueflow.businessstaff.BusinessStaffRepository;
import com.queueflow.queue.QueueRepository;
import com.queueflow.service.ServiceRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private BusinessRepository businessRepository;

    @Autowired
    private BusinessStaffRepository businessStaffRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        appointmentRepository.deleteAll();
        queueRepository.deleteAll();
        serviceRepository.deleteAll();
        businessStaffRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterCustomer() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "StrongPassword123",
                "9876543210",
                UserRole.CUSTOMER
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
                "8888888888",
                UserRole.CUSTOMER
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
                "123",
                UserRole.CUSTOMER
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
                null,
                UserRole.CUSTOMER
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
                null,
                UserRole.CUSTOMER
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

    @Test
    void shouldCompleteRegisterLoginAndAccessProtectedEndpoint() throws Exception {
    
        RegisterRequest request = new RegisterRequest(
                "E2E Customer",
                "e2e-customer@example.com",
                "StrongPassword123",
                "9876543210",
                UserRole.CUSTOMER
        );
    
        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated());
    
        String loginResponse = mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "e2e-customer@example.com",
                                    "password": "StrongPassword123"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("e2e-customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn()
                .getResponse()
                .getContentAsString();
    
        JsonNode loginJson =
                objectMapper.readTree(loginResponse);
    
        String accessToken =
                loginJson.get("accessToken").asText();
    
        assertNotNull(accessToken);
    
        Long customerId = userRepository
                .findByEmail("e2e-customer@example.com")
                .orElseThrow()
                .getId();
    
        mockMvc.perform(
                get(
                        "/api/v1/appointments/customers/{customerId}",
                        customerId
                )
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectProtectedEndpointWithoutAuthentication()
            throws Exception {
    
        User customer = new User();
        customer.setName("Protected Customer");
        customer.setEmail("protected-customer@example.com");
        customer.setPhone("9999999999");
        customer.setPasswordHash(
                passwordEncoder.encode("StrongPassword123")
        );
        customer.setRole(UserRole.CUSTOMER);
    
        java.time.LocalDateTime now =
                java.time.LocalDateTime.now();
    
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
    
        customer = userRepository.saveAndFlush(customer);
    
        mockMvc.perform(
                get(
                        "/api/v1/appointments/customers/{customerId}",
                        customer.getId()
                )
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectCustomerFromAccessingAnotherCustomersData()
            throws Exception {
    
        RegisterRequest firstCustomer = new RegisterRequest(
                "First Customer",
                "first-customer@example.com",
                "StrongPassword123",
                null,
                UserRole.CUSTOMER
        );
    
        RegisterRequest secondCustomer = new RegisterRequest(
                "Second Customer",
                "second-customer@example.com",
                "StrongPassword123",
                null,
                UserRole.CUSTOMER
        );
    
        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(firstCustomer)
                        )
        )
                .andExpect(status().isCreated());
    
        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(secondCustomer)
                        )
        )
                .andExpect(status().isCreated());
    
        String loginResponse = mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "first-customer@example.com",
                                    "password": "StrongPassword123"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    
        String accessToken =
                objectMapper
                        .readTree(loginResponse)
                        .get("accessToken")
                        .asText();
    
        Long secondCustomerId = userRepository
                .findByEmail("second-customer@example.com")
                .orElseThrow()
                .getId();
    
        mockMvc.perform(
                get(
                        "/api/v1/appointments/customers/{customerId}",
                        secondCustomerId
                )
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
        )
                .andExpect(status().isForbidden());
    }
    

    @Test
    void shouldRegisterBusinessOwner() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Business Owner",
                "business-owner@example.com",
                "StrongPassword123",
                "9876543210",
                UserRole.BUSINESS_OWNER
        );

        String response = mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.name").value("Business Owner"))
                .andExpect(jsonPath("$.email").value("business-owner@example.com"))
                .andExpect(jsonPath("$.role").value("BUSINESS_OWNER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long userId = objectMapper
                .readTree(response)
                .get("userId")
                .asLong();

        User user = userRepository.findById(userId).orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(
                UserRole.BUSINESS_OWNER,
                user.getRole()
        );
    }

    @Test
    void shouldRejectStaffRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Staff User",
                "staff@example.com",
                "StrongPassword123",
                "9876543210",
                UserRole.STAFF
        );

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectAdminRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Admin User",
                "admin@example.com",
                "StrongPassword123",
                "9876543210",
                UserRole.ADMIN
        );

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRegistrationWithoutRole() throws Exception {
        String request = """
                {
                    "name": "Missing Role",
                    "email": "missing-role@example.com",
                    "password": "StrongPassword123",
                    "phone": "9876543210"
                }
                """;

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

}