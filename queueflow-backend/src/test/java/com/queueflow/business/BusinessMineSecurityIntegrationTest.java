package com.queueflow.business;

import tools.jackson.databind.ObjectMapper;

import com.queueflow.auth.LoginRequest;
import com.queueflow.business.dto.CreateBusinessRequest;
import com.queueflow.businessstaff.dto.AssignStaffRequest;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "queueflow.security.enabled=true")
@AutoConfigureMockMvc
@Transactional
class BusinessMineSecurityIntegrationTest {

    private static final String PASSWORD = "TestPassword123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldReturnOwnedBusinessesForBusinessOwner() throws Exception {
        User owner = createUser(
                "Mine Owner",
                "mine-owner@test.com",
                UserRole.BUSINESS_OWNER
        );

        Long businessId = createBusiness(
                owner.getId(),
                "Owner Business"
        );

        String token = loginAndGetToken(
                "mine-owner@test.com"
        );

        mockMvc.perform(
                get("/api/v1/businesses/mine")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(businessId))
                .andExpect(jsonPath("$.content[0].name").value("Owner Business"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnAssignedBusinessesForStaff() throws Exception {
        User owner = createUser(
                "Staff Business Owner",
                "mine-staff-owner@test.com",
                UserRole.BUSINESS_OWNER
        );

        Long businessId = createBusiness(
                owner.getId(),
                "Staff Assigned Business"
        );

        User staff = createUser(
                "Mine Staff",
                "mine-staff@test.com",
                UserRole.STAFF
        );

        assignStaff(businessId, staff.getId());

        String token = loginAndGetToken(
                "mine-staff@test.com"
        );

        mockMvc.perform(
                get("/api/v1/businesses/mine")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(businessId))
                .andExpect(jsonPath("$.content[0].name")
                        .value("Staff Assigned Business"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldRejectCustomerFromAccessingMyBusinesses() throws Exception {
        createUser(
                "Mine Customer",
                "mine-customer@test.com",
                UserRole.CUSTOMER
        );

        String token = loginAndGetToken(
                "mine-customer@test.com"
        );

        mockMvc.perform(
                get("/api/v1/businesses/mine")
                        .header("Authorization", "Bearer " + token)
        )
                .andExpect(status().isForbidden());
    }

    private User createUser(
            String name,
            String email,
            UserRole role
    ) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone("9999999999");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        return userRepository.save(user);
    }

    private Long createBusiness(
            Long ownerId,
            String name
    ) throws Exception {
        User owner = userRepository.findById(ownerId)
                .orElseThrow();

        String token = loginAndGetToken(owner.getEmail());

        CreateBusinessRequest request = new CreateBusinessRequest();
        request.setOwnerId(ownerId);
        request.setName(name);
        request.setDescription("Test business");
        request.setAddress("Hyderabad");
        request.setTimezone("Asia/Kolkata");

        String response = mockMvc.perform(
                post("/api/v1/businesses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("id")
                .asLong();
    }

    private void assignStaff(
            Long businessId,
            Long staffId
    ) throws Exception {
        User owner = userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == UserRole.BUSINESS_OWNER)
                .findFirst()
                .orElseThrow();

        String token = loginAndGetToken(owner.getEmail());

        AssignStaffRequest request = new AssignStaffRequest();
        request.setStaffId(staffId);

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/staff", businessId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String email) throws Exception {
        LoginRequest request = new LoginRequest(
                email,
                PASSWORD
        );

        String response = mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("accessToken")
                .asText();
    }
}
