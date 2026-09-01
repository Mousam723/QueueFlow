package com.queueflow.queue;

import tools.jackson.databind.ObjectMapper;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.business.BusinessStatus;
import com.queueflow.queue.dto.CreateQueueRequest;
import com.queueflow.service.Service;
import com.queueflow.service.ServiceRepository;
import com.queueflow.service.ServiceStatus;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QueueControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private Business createBusiness(String email) {

        User owner = new User();

        owner.setName("Test Owner");
        owner.setEmail(email);
        owner.setPhone("9999999999");
        owner.setPasswordHash("test-password");
        owner.setRole(UserRole.BUSINESS_OWNER);

        User savedOwner = userRepository.save(owner);

        Business business = new Business();

        business.setOwner(savedOwner);
        business.setName("Test Clinic");
        business.setDescription("Test clinic");
        business.setAddress("Hyderabad");
        business.setTimezone("Asia/Kolkata");
        business.setStatus(BusinessStatus.ACTIVE);

        return businessRepository.save(business);
    }

    private Service createService(Business business) {

        Service service = new Service();

        service.setBusiness(business);
        service.setName("General Consultation");
        service.setDescription("General consultation service");
        service.setDurationMinutes(30);
        service.setStatus(ServiceStatus.ACTIVE);

        return serviceRepository.save(service);
    }

    @Test
    void shouldCreateQueueSuccessfully() throws Exception {

        Business business = createBusiness("queue-owner-1@test.com");
        Service service = createService(business);

        CreateQueueRequest request = new CreateQueueRequest();

        request.setServiceId(service.getId());
        request.setQueueDate(LocalDate.of(2026, 8, 31));

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.businessId").value(business.getId()))
        .andExpect(jsonPath("$.serviceId").value(service.getId()))
        .andExpect(jsonPath("$.queueDate").value("2026-08-31"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.currentToken").value(0));
    }

    @Test
    void shouldCreateQueueForTodayWhenDateIsNotProvided() throws Exception {

        Business business = createBusiness("queue-owner-2@test.com");
        Service service = createService(business);

        CreateQueueRequest request = new CreateQueueRequest();

        request.setServiceId(service.getId());

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.queueDate").value(LocalDate.now().toString()))
        .andExpect(jsonPath("$.currentToken").value(0));
    }

    @Test
    void shouldReturn404WhenBusinessDoesNotExist() throws Exception {

        CreateQueueRequest request = new CreateQueueRequest();

        request.setServiceId(1L);
        request.setQueueDate(LocalDate.of(2026, 8, 31));

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldReturn404WhenServiceDoesNotExist() throws Exception {

        Business business = createBusiness("queue-owner-3@test.com");

        CreateQueueRequest request = new CreateQueueRequest();

        request.setServiceId(999999L);
        request.setQueueDate(LocalDate.of(2026, 8, 31));

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRejectServiceBelongingToAnotherBusiness() throws Exception {

        Business business1 = createBusiness("queue-owner-4@test.com");
        Business business2 = createBusiness("queue-owner-5@test.com");

        Service serviceOfBusiness2 = createService(business2);

        CreateQueueRequest request = new CreateQueueRequest();

        request.setServiceId(serviceOfBusiness2.getId());
        request.setQueueDate(LocalDate.of(2026, 8, 31));

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateQueueForSameServiceAndDate() throws Exception {

        Business business = createBusiness("queue-owner-6@test.com");
        Service service = createService(business);

        LocalDate date = LocalDate.of(2026, 8, 31);

        CreateQueueRequest request = new CreateQueueRequest();

        request.setServiceId(service.getId());
        request.setQueueDate(date);

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isConflict());
    }

    @Test
    void shouldGetBusinessQueues() throws Exception {

        Business business = createBusiness("queue-owner-7@test.com");
        Service service = createService(business);

        LocalDate date = LocalDate.of(2026, 8, 31);

        CreateQueueRequest request = new CreateQueueRequest();

        request.setServiceId(service.getId());
        request.setQueueDate(date);

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated());

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}/queues", business.getId())
                        .param("date", date.toString())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].serviceId").value(service.getId()))
        .andExpect(jsonPath("$[0].queueDate").value(date.toString()));
    }

    @Test
    void shouldReturn404WhenGettingQueuesForNonExistingBusiness()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}/queues", 999999L)
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }
}