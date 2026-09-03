package com.queueflow.queue;
import tools.jackson.databind.JsonNode;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    /*
     * ============================================================
     * TEST DATA HELPERS
     * ============================================================
     */

    private Business createBusiness(String email) {

        User owner = new User();
        owner.setName("Queue Test Owner");
        owner.setEmail(email);
        owner.setPhone("9999999999");
        owner.setPasswordHash("test-password");
        owner.setRole(UserRole.BUSINESS_OWNER);

        owner = userRepository.save(owner);

        Business business = new Business();
        business.setOwner(owner);
        business.setName("Test Business");
        business.setDescription("Business created for queue integration tests");
        business.setAddress("Hyderabad");
        business.setTimezone("Asia/Kolkata");
        business.setStatus(BusinessStatus.ACTIVE);

        return businessRepository.save(business);
    }

    private Service createService(Business business) {

        Service service = new Service();
        service.setBusiness(business);
        service.setName("Test Service");
        service.setDescription("Service created for queue integration tests");
        service.setDurationMinutes(30);
        service.setStatus(ServiceStatus.ACTIVE);

        return serviceRepository.save(service);
    }

    private Long createQueue(
            Business business,
            Service service,
            LocalDate queueDate
    ) throws Exception {

        CreateQueueRequest request = new CreateQueueRequest();
        request.setServiceId(service.getId());
        request.setQueueDate(queueDate);

        String response = mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);

        Long queueId = jsonNode.get("id").asLong();

        assertNotNull(queueId);

        return queueId;
    }

    /*
     * ============================================================
     * QUEUE CREATION
     * ============================================================
     */

    @Test
    void shouldCreateQueueForBusinessAndService() throws Exception {

        Business business = createBusiness(
                "queue-create@test.com"
        );

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
    void shouldDefaultQueueDateToToday() throws Exception {

        Business business = createBusiness(
                "queue-default-date@test.com"
        );

        Service service = createService(business);

        CreateQueueRequest request = new CreateQueueRequest();
        request.setServiceId(service.getId());

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueDate")
                        .value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentToken").value(0));
    }

    @Test
    void shouldRejectQueueForNonExistentBusiness() throws Exception {

        CreateQueueRequest request = new CreateQueueRequest();
        request.setServiceId(999999L);
        request.setQueueDate(LocalDate.of(2026, 8, 31));

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectQueueForNonExistentService() throws Exception {

        Business business = createBusiness(
                "queue-invalid-service@test.com"
        );

        CreateQueueRequest request = new CreateQueueRequest();
        request.setServiceId(999999L);
        request.setQueueDate(LocalDate.of(2026, 8, 31));

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectServiceThatDoesNotBelongToBusiness() throws Exception {

        Business businessOne = createBusiness(
                "queue-business-one@test.com"
        );

        Business businessTwo = createBusiness(
                "queue-business-two@test.com"
        );

        Service service = createService(businessOne);

        CreateQueueRequest request = new CreateQueueRequest();
        request.setServiceId(service.getId());
        request.setQueueDate(LocalDate.of(2026, 8, 31));

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", businessTwo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateQueueForSameServiceAndDate() throws Exception {

        Business business = createBusiness(
                "queue-duplicate@test.com"
        );

        Service service = createService(business);

        LocalDate queueDate = LocalDate.of(2026, 8, 31);

        createQueue(
                business,
                service,
                queueDate
        );

        CreateQueueRequest duplicateRequest = new CreateQueueRequest();
        duplicateRequest.setServiceId(service.getId());
        duplicateRequest.setQueueDate(queueDate);

        mockMvc.perform(
                post("/api/v1/businesses/{businessId}/queues", business.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest))
        )
                .andExpect(status().isConflict());
    }

    /*
     * ============================================================
     * QUEUE RETRIEVAL
     * ============================================================
     */

    @Test
    void shouldGetQueueById() throws Exception {

        Business business = createBusiness(
                "queue-get@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                get("/api/v1/queues/{queueId}", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queueId))
                .andExpect(jsonPath("$.businessId").value(business.getId()))
                .andExpect(jsonPath("$.serviceId").value(service.getId()))
                .andExpect(jsonPath("$.queueDate").value("2026-08-31"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentToken").value(0));
    }

    @Test
    void shouldReturn404ForNonExistentQueue() throws Exception {

        mockMvc.perform(
                get("/api/v1/queues/{queueId}", 999999L)
        )
                .andExpect(status().isNotFound());
    }

    /*
     * ============================================================
     * BUSINESS QUEUE LISTING
     * ============================================================
     */

    @Test
    void shouldListQueuesForBusinessAndDate() throws Exception {

        Business business = createBusiness(
                "queue-list@test.com"
        );

        Service serviceOne = createService(business);
        Service serviceTwo = createService(business);

        LocalDate queueDate = LocalDate.of(2026, 8, 31);

        createQueue(
                business,
                serviceOne,
                queueDate
        );

        createQueue(
                business,
                serviceTwo,
                queueDate
        );

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}/queues", business.getId())
                        .param("date", queueDate.toString())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /*
     * ============================================================
     * QUEUE TOKEN ADVANCEMENT
     * ============================================================
     */

    @Test
    void shouldAdvanceActiveQueue() throws Exception {

        Business business = createBusiness(
                "queue-next@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queueId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentToken").value(1));
    }

    @Test
    void shouldAdvanceQueueMultipleTimes() throws Exception {

        Business business = createBusiness(
                "queue-next-multiple@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentToken").value(1));

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentToken").value(2));

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentToken").value(3));
    }

    @Test
    void shouldReturn404WhenAdvancingNonExistentQueue() throws Exception {

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", 999999L)
        )
                .andExpect(status().isNotFound());
    }

    /*
     * ============================================================
     * PAUSE / RESUME
     * ============================================================
     */

    @Test
    void shouldPauseActiveQueue() throws Exception {

        Business business = createBusiness(
                "queue-pause@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/pause", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queueId))
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.currentToken").value(0));
    }

    @Test
    void shouldResumePausedQueue() throws Exception {

        Business business = createBusiness(
                "queue-resume@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/pause", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/resume", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queueId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentToken").value(0));
    }

    /*
     * ============================================================
     * CLOSE
     * ============================================================
     */

    @Test
    void shouldCloseActiveQueue() throws Exception {

        Business business = createBusiness(
                "queue-close@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/close", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queueId))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void shouldClosePausedQueue() throws Exception {

        Business business = createBusiness(
                "queue-close-paused@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/pause", queueId)
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/close", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    /*
     * ============================================================
     * INVALID STATE TRANSITIONS
     * ============================================================
     */

    @Test
    void shouldRejectAdvancingPausedQueue() throws Exception {

        Business business = createBusiness(
                "queue-paused-next@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/pause", queueId)
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", queueId)
        )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectResumingActiveQueue() throws Exception {

        Business business = createBusiness(
                "queue-active-resume@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/resume", queueId)
        )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectPausingPausedQueue() throws Exception {

        Business business = createBusiness(
                "queue-paused-pause@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/pause", queueId)
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/pause", queueId)
        )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectOperationsOnClosedQueue() throws Exception {

        Business business = createBusiness(
                "queue-closed-operations@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/close", queueId)
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", queueId)
        )
                .andExpect(status().isConflict());

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/pause", queueId)
        )
                .andExpect(status().isConflict());

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/resume", queueId)
        )
                .andExpect(status().isConflict());

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/close", queueId)
        )
                .andExpect(status().isConflict());
    }

    /*
     * ============================================================
     * QUEUE STATE + TOKEN PRESERVATION
     * ============================================================
     */

    @Test
    void shouldPreserveTokenWhenQueueIsPausedAndResumed() throws Exception {

        Business business = createBusiness(
                "queue-token-state@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentToken").value(1));

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/pause", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.currentToken").value(1));

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/resume", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentToken").value(1));

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentToken").value(2));
    }

    @Test
    void shouldPreserveTokenWhenClosingQueue() throws Exception {

        Business business = createBusiness(
                "queue-token-close@test.com"
        );

        Service service = createService(business);

        Long queueId = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/next", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentToken").value(1));

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/close", queueId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.currentToken").value(1));
    }
}
