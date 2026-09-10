package com.queueflow.business;

import tools.jackson.databind.ObjectMapper;
import com.queueflow.business.dto.CreateBusinessRequest;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BusinessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateBusinessSuccessfully() throws Exception {

        User owner = new User();
        owner.setName("Test Owner");
        owner.setEmail("owner@test.com");
        owner.setPhone("9999999999");
        owner.setPasswordHash("test-password");
        owner.setRole(UserRole.BUSINESS_OWNER);

        User savedOwner = userRepository.save(owner);

        CreateBusinessRequest request = new CreateBusinessRequest();
        request.setOwnerId(savedOwner.getId());
        request.setName("Test Clinic");
        request.setDescription("General medical consultation");
        request.setAddress("Hyderabad");
        request.setTimezone("Asia/Kolkata");

        mockMvc.perform(
                post("/api/v1/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Test Clinic"))
        .andExpect(jsonPath("$.description")
                .value("General medical consultation"))
        .andExpect(jsonPath("$.address").value("Hyderabad"))
        .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn404WhenOwnerDoesNotExist() throws Exception {

        CreateBusinessRequest request = new CreateBusinessRequest();
        request.setOwnerId(999999L);
        request.setName("Test Clinic");
        request.setDescription("Testing");
        request.setAddress("Hyderabad");
        request.setTimezone("Asia/Kolkata");

        mockMvc.perform(
                post("/api/v1/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.message")
                .value("Business owner with id 999999 was not found"));
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception {

        CreateBusinessRequest request = new CreateBusinessRequest();

        mockMvc.perform(
                post("/api/v1/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
void shouldGetBusinessSuccessfully() throws Exception {
    User owner = new User();
    owner.setName("Get Business Owner");
    owner.setEmail("get-business-owner@test.com");
    owner.setPhone("9999999999");
    owner.setPasswordHash("test-password");
    owner.setRole(UserRole.BUSINESS_OWNER);

    User savedOwner = userRepository.save(owner);

    CreateBusinessRequest request = new CreateBusinessRequest();
    request.setOwnerId(savedOwner.getId());
    request.setName("Test Clinic");
    request.setDescription("General medical consultation");
    request.setAddress("Hyderabad");
    request.setTimezone("Asia/Kolkata");

    String response = mockMvc.perform(
            post("/api/v1/businesses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long businessId = objectMapper.readTree(response)
            .get("id")
            .asLong();

    mockMvc.perform(
            get("/api/v1/businesses/{businessId}", businessId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(businessId))
            .andExpect(jsonPath("$.name").value("Test Clinic"))
            .andExpect(jsonPath("$.description")
                    .value("General medical consultation"))
            .andExpect(jsonPath("$.address").value("Hyderabad"))
            .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
}

@Test
void shouldReturn404WhenBusinessDoesNotExist() throws Exception {
    mockMvc.perform(
            get("/api/v1/businesses/{businessId}", 999999L)
    )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error")
                    .value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message")
                    .value("Business with id 999999 was not found"));
}
}
