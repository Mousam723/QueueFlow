package com.queueflow.appointment;

import com.queueflow.appointment.dto.CreateAppointmentRequest;
import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.business.BusinessStatus;
import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueRepository;
import com.queueflow.queue.QueueStatus;
import com.queueflow.queue.dto.CreateQueueRequest;
import com.queueflow.service.Service;
import com.queueflow.service.ServiceRepository;
import com.queueflow.service.ServiceStatus;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerIntegrationTest {

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

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @AfterEach
    void cleanupTestData() {
        appointmentRepository.deleteAll();
        queueRepository.deleteAll();
        serviceRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();
    }

    /*
     * ============================================================
     * TEST DATA HELPERS
     * ============================================================
     */

    private User createCustomer(String email) {
        User customer = new User();

        customer.setName("Appointment Test Customer");
        customer.setEmail(email);
        customer.setPhone("8888888888");
        customer.setPasswordHash("test-password");
        customer.setRole(UserRole.CUSTOMER);

        return userRepository.save(customer);
    }

    private Business createBusiness(String email) {
        User owner = new User();

        owner.setName("Appointment Test Owner");
        owner.setEmail(email);
        owner.setPhone("9999999999");
        owner.setPasswordHash("test-password");
        owner.setRole(UserRole.BUSINESS_OWNER);

        owner = userRepository.save(owner);

        Business business = new Business();

        business.setOwner(owner);
        business.setName("Appointment Test Business");
        business.setDescription(
                "Business created for appointment integration tests"
        );
        business.setAddress("Hyderabad");
        business.setTimezone("Asia/Kolkata");
        business.setStatus(BusinessStatus.ACTIVE);

        return businessRepository.save(business);
    }

    private Service createService(Business business) {
        Service service = new Service();

        service.setBusiness(business);
        service.setName("Appointment Test Service");
        service.setDescription(
                "Service created for appointment integration tests"
        );
        service.setDurationMinutes(30);
        service.setStatus(ServiceStatus.ACTIVE);

        return serviceRepository.save(service);
    }

    private Queue createQueue(
            Business business,
            Service service,
            LocalDate queueDate
    ) throws Exception {

        CreateQueueRequest request = new CreateQueueRequest();

        request.setServiceId(service.getId());
        request.setQueueDate(queueDate);

        String response = mockMvc.perform(
                post(
                        "/api/v1/businesses/{businessId}/queues",
                        business.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);

        Long queueId = jsonNode.get("id").asLong();

        assertNotNull(queueId);

        return queueRepository.findById(queueId)
                .orElseThrow();
    }

    private String createAppointmentRequest(
            Long customerId,
            Long queueId
    ) throws Exception {

        CreateAppointmentRequest request =
                new CreateAppointmentRequest();

        request.setCustomerId(customerId);
        request.setQueueId(queueId);

        return objectMapper.writeValueAsString(request);
    }

    /*
     * ============================================================
     * APPOINTMENT CREATION
     * ============================================================
     */

    @Test
    void shouldCreateAppointmentWithFirstToken() throws Exception {

        User customer = createCustomer(
                "appointment-create@test.com"
        );

        Business business = createBusiness(
                "appointment-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                createAppointmentRequest(
                                        customer.getId(),
                                        queue.getId()
                                )
                        )
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(
                        jsonPath("$.customerId")
                                .value(customer.getId())
                )
                .andExpect(
                        jsonPath("$.businessId")
                                .value(business.getId())
                )
                .andExpect(
                        jsonPath("$.serviceId")
                                .value(service.getId())
                )
                .andExpect(
                        jsonPath("$.queueId")
                                .value(queue.getId())
                )
                .andExpect(
                        jsonPath("$.tokenNumber")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.appointmentDate")
                                .value("2026-08-31")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("WAITING")
                );
    }

    @Test
    void shouldAssignSequentialTokens() throws Exception {

        User customerOne = createCustomer(
                "appointment-sequential-1@test.com"
        );

        User customerTwo = createCustomer(
                "appointment-sequential-2@test.com"
        );

        Business business = createBusiness(
                "appointment-sequential-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                createAppointmentRequest(
                                        customerOne.getId(),
                                        queue.getId()
                                )
                        )
        )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.tokenNumber").value(1)
                );

        mockMvc.perform(
                post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                createAppointmentRequest(
                                        customerTwo.getId(),
                                        queue.getId()
                                )
                        )
        )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.tokenNumber").value(2)
                );
    }

    /*
     * ============================================================
     * VALIDATION / ERROR CASES
     * ============================================================
     */

    @Test
    void shouldRejectAppointmentForNonExistentCustomer()
            throws Exception {

        Business business = createBusiness(
                "appointment-missing-customer-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                createAppointmentRequest(
                                        999999L,
                                        queue.getId()
                                )
                        )
        )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAppointmentForNonExistentQueue()
            throws Exception {

        User customer = createCustomer(
                "appointment-missing-queue@test.com"
        );

        mockMvc.perform(
                post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                createAppointmentRequest(
                                        customer.getId(),
                                        999999L
                                )
                        )
        )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAppointmentOnPausedQueue()
            throws Exception {

        User customer = createCustomer(
                "appointment-paused@test.com"
        );

        Business business = createBusiness(
                "appointment-paused-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/pause", queue.getId())
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                createAppointmentRequest(
                                        customer.getId(),
                                        queue.getId()
                                )
                        )
        )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectAppointmentOnClosedQueue()
            throws Exception {

        User customer = createCustomer(
                "appointment-closed@test.com"
        );

        Business business = createBusiness(
                "appointment-closed-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post("/api/v1/queues/{queueId}/close", queue.getId())
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                createAppointmentRequest(
                                        customer.getId(),
                                        queue.getId()
                                )
                        )
        )
                .andExpect(status().isConflict());
    }

    /*
     * ============================================================
     * CONCURRENCY
     * ============================================================
     */

    @Test
    void shouldSafelyIssueConcurrentAppointmentTokens()
            throws Exception {

        Business business = createBusiness(
                "appointment-concurrency-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        int requestCount = 10;

        List<User> customers = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            customers.add(
                    createCustomer(
                            "appointment-concurrency-" + i + "@test.com"
                    )
            );
        }

        ExecutorService executorService =
                Executors.newFixedThreadPool(requestCount);

        try {
            List<Callable<Integer>> tasks = new ArrayList<>();

            for (User customer : customers) {
                tasks.add(() -> {

                    String response = mockMvc.perform(
                            post("/api/v1/appointments")
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            createAppointmentRequest(
                                                    customer.getId(),
                                                    queue.getId()
                                            )
                                    )
                    )
                            .andExpect(status().isCreated())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

                    JsonNode jsonNode =
                            objectMapper.readTree(response);

                    return jsonNode
                            .get("tokenNumber")
                            .asInt();
                });
            }

            List<Future<Integer>> results =
                    executorService.invokeAll(tasks);

            List<Integer> tokens = new ArrayList<>();

            for (Future<Integer> result : results) {
                tokens.add(result.get());
            }

            assertEquals(
                    requestCount,
                    tokens.size()
            );

            assertEquals(
                    requestCount,
                    tokens.stream().distinct().count()
            );

            assertEquals(
                    requestCount,
                    tokens.stream()
                            .mapToInt(Integer::intValue)
                            .max()
                            .orElse(0)
            );

        } finally {
            executorService.shutdown();
        }
    }

    @Test
void shouldRejectAppointmentForNonCustomerUser() throws Exception {
    User businessOwner = new User();
    businessOwner.setName("Non Customer User");
    businessOwner.setEmail("appointment-non-customer@test.com");
    businessOwner.setPhone("7777777777");
    businessOwner.setPasswordHash("test-password");
    businessOwner.setRole(UserRole.BUSINESS_OWNER);
    businessOwner = userRepository.save(businessOwner);

    Business business = createBusiness("appointment-role-business@test.com");
    Service service = createService(business);
    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    mockMvc.perform(
            post("/api/v1/appointments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            createAppointmentRequest(
                                    businessOwner.getId(),
                                    queue.getId()
                            )
                    )
    )
            .andExpect(status().isConflict());
}
}