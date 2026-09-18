package com.queueflow.service;

import tools.jackson.databind.ObjectMapper;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.business.BusinessStatus;
import com.queueflow.service.dto.CreateServiceRequest;
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
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ServiceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessRepository businessRepository;

    private Business createTestBusiness() {

        User owner = new User();
        owner.setName("Test Owner");
        owner.setEmail("service-owner@test.com");
        owner.setPhone("9999999999");
        owner.setPasswordHash("test-password");
        owner.setRole(UserRole.BUSINESS_OWNER);

        User savedOwner = userRepository.save(owner);

        Business business = new Business();
        business.setOwner(savedOwner);
        business.setName("Test Clinic");
        business.setDescription("Test business");
        business.setAddress("Hyderabad");
        business.setTimezone("Asia/Kolkata");
        business.setStatus(BusinessStatus.ACTIVE);

        return businessRepository.save(business);
    }

    @Test
    void shouldCreateServiceSuccessfully() throws Exception {

        Business business = createTestBusiness();

        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("General Consultation");
        request.setDescription("30 minute doctor consultation");
        request.setDurationMinutes(30);

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/services", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.businessId").value(business.getId()))
        .andExpect(jsonPath("$.name").value("General Consultation"))
        .andExpect(jsonPath("$.description")
                .value("30 minute doctor consultation"))
        .andExpect(jsonPath("$.durationMinutes").value(30))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn404WhenBusinessDoesNotExist() throws Exception {

        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("General Consultation");
        request.setDescription("Testing");
        request.setDurationMinutes(30);

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/services", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.message")
                .value("Business with id 999999 was not found"));
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception {

        Business business = createTestBusiness();

        CreateServiceRequest request = new CreateServiceRequest();

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/services", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldGetServicesForBusiness() throws Exception {

        Business business = createTestBusiness();

        CreateServiceRequest firstRequest = new CreateServiceRequest();
        firstRequest.setName("General Consultation");
        firstRequest.setDescription("General consultation");
        firstRequest.setDurationMinutes(30);

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/services", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest))
        )
        .andExpect(status().isCreated());

        CreateServiceRequest secondRequest = new CreateServiceRequest();
        secondRequest.setName("Follow-up Consultation");
        secondRequest.setDescription("Follow-up consultation");
        secondRequest.setDurationMinutes(15);

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/services", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest))
        )
        .andExpect(status().isCreated());

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}/services", business.getId())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("General Consultation"))
        .andExpect(jsonPath("$[1].name").value("Follow-up Consultation"));
    }

    @Test
    void shouldReturn404WhenGettingServicesForNonExistingBusiness()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}/services", 999999L)
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.message")
                .value("Business with id 999999 was not found"));
    }
}
