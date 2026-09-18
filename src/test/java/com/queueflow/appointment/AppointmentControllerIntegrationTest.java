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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.MvcResult;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
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

    private RequestBuilder createAppointmentPost(
            Long customerId,
            Long queueId
    ) throws Exception {
        return post("/api/v1/appointments")
                .header(
                        "Idempotency-Key",
                        java.util.UUID.randomUUID().toString()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        createAppointmentRequest(
                                customerId,
                                queueId
                        )
                );
    }

    private RequestBuilder createAppointmentPost(
        Long customerId,
        Long queueId,
        String idempotencyKey
    ) throws Exception {
        return post("/api/v1/appointments")
            .header(
                    "Idempotency-Key",
                    idempotencyKey
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                    createAppointmentRequest(
                            customerId,
                            queueId
                    )
            );
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
                createAppointmentPost(
                        customer.getId(),
                        queue.getId()
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
void shouldReturnExistingAppointmentForSameIdempotencyKeyAndSamePayload()
        throws Exception {

    User customer = createCustomer(
            "appointment-idempotency-same@test.com"
    );

    Business business = createBusiness(
            "appointment-idempotency-same-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String idempotencyKey = "idempotency-same-key";

    String firstResponse =
            mockMvc.perform(
                    createAppointmentPost(
                            customer.getId(),
                            queue.getId(),
                            idempotencyKey
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(
                    jsonPath("$.tokenNumber")
                            .value(1)
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

    String secondResponse =
            mockMvc.perform(
                    createAppointmentPost(
                            customer.getId(),
                            queue.getId(),
                            idempotencyKey
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(
                    jsonPath("$.tokenNumber")
                            .value(1)
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

    long firstId =
            objectMapper.readTree(firstResponse)
                    .get("id")
                    .asLong();

    long secondId =
            objectMapper.readTree(secondResponse)
                    .get("id")
                    .asLong();

    org.junit.jupiter.api.Assertions.assertEquals(
            firstId,
            secondId
    );
}

@Test
void shouldRejectSameIdempotencyKeyWithDifferentPayload()
        throws Exception {

    User customerOne = createCustomer(
            "appointment-idempotency-different-1@test.com"
    );

    User customerTwo = createCustomer(
            "appointment-idempotency-different-2@test.com"
    );

    Business business = createBusiness(
            "appointment-idempotency-different-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String idempotencyKey = "idempotency-different-key";

    mockMvc.perform(
            createAppointmentPost(
                    customerOne.getId(),
                    queue.getId(),
                    idempotencyKey
            )
    )
            .andExpect(status().isCreated());

    mockMvc.perform(
            createAppointmentPost(
                    customerTwo.getId(),
                    queue.getId(),
                    idempotencyKey
            )
    )
            .andExpect(status().isConflict())
            .andExpect(
                    jsonPath("$.message")
                            .value(
                                    "Idempotency-Key has already been used "
                                            + "with a different request payload"
                            )
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
                createAppointmentPost(
                        customerOne.getId(),
                        queue.getId()
                )
        )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.tokenNumber").value(1)
                );

        mockMvc.perform(
                createAppointmentPost(
                        customerTwo.getId(),
                        queue.getId()
                )
        )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.tokenNumber").value(2)
                );
    }

    @Test
void shouldSafelyAllocateUniqueTokensWithConcurrentRequests()
        throws Exception {

    Business business = createBusiness(
            "appointment-concurrent-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    List<User> customers = new ArrayList<>();

    for (int i = 1; i <= 10; i++) {
        customers.add(
                createCustomer(
                        "appointment-concurrent-" + i + "@test.com"
                )
        );
    }

    ExecutorService executorService =
            Executors.newFixedThreadPool(10);

    try {
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (User customer : customers) {
            tasks.add(() -> {
                String response = mockMvc.perform(
                        createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
                )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                JsonNode jsonNode =
                        objectMapper.readTree(response);

                return jsonNode.get("tokenNumber").asInt();
            });
        }

        List<Future<Integer>> futures =
                executorService.invokeAll(tasks);

        List<Integer> tokens = new ArrayList<>();

        for (Future<Integer> future : futures) {
            tokens.add(future.get());
        }

        assertEquals(10, tokens.size());
        assertEquals(10, tokens.stream().distinct().count());

        tokens.sort(Integer::compareTo);

        assertEquals(
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                tokens
        );

        Queue finalQueue =
                queueRepository.findById(queue.getId())
                        .orElseThrow();

        assertEquals(0, finalQueue.getCurrentToken());
        assertEquals(10, finalQueue.getNextTokenNumber());

        List<Appointment> appointments =
                appointmentRepository
                        .findByQueueIdOrderByTokenNumberAsc(
                                queue.getId()
                        );

        assertEquals(10, appointments.size());

        for (int i = 0; i < appointments.size(); i++) {
            Appointment appointment = appointments.get(i);

            assertEquals(i + 1, appointment.getTokenNumber());
            assertEquals(
                    AppointmentStatus.WAITING,
                    appointment.getStatus()
            );
        }

    } finally {
        executorService.shutdown();
    }
}

@Test
void shouldMaintainConsistencyWhenBookingAndAdvancingConcurrently()
        throws Exception {

    Business business = createBusiness(
            "appointment-race-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    // Create one initial appointment so the queue
    // always has something available to serve.
    User initialCustomer = createCustomer(
            "appointment-race-initial@test.com"
    );

    mockMvc.perform(
            createAppointmentPost(
                        initialCustomer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenNumber").value(1))
            .andExpect(jsonPath("$.status").value("WAITING"));

    List<User> concurrentCustomers = new ArrayList<>();

    for (int i = 1; i <= 10; i++) {
        concurrentCustomers.add(
                createCustomer(
                        "appointment-race-" + i + "@test.com"
                )
        );
    }

    ExecutorService executorService =
            Executors.newFixedThreadPool(20);

    try {
        List<Callable<Integer>> bookingTasks =
                new ArrayList<>();

        for (User customer : concurrentCustomers) {
            bookingTasks.add(() -> {
                String response = mockMvc.perform(
                        createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
                )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                JsonNode jsonNode =
                        objectMapper.readTree(response);

                return jsonNode.get("tokenNumber").asInt();
            });
        }

        List<Callable<Integer>> advanceTasks =
                new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            advanceTasks.add(() -> {
                var result = mockMvc.perform(
                        post(
                                "/api/v1/queues/{queueId}/next",
                                queue.getId()
                        )
                ).andReturn();

                int status = result.getResponse().getStatus();

                if (status == 409) {
                    return -1;
                }

                assertEquals(200, status);

                JsonNode jsonNode =
                        objectMapper.readTree(
                                result.getResponse()
                                        .getContentAsString()
                        );

                return jsonNode.get("currentToken").asInt();
            });
        }

        List<Callable<Integer>> allTasks =
                new ArrayList<>();

        allTasks.addAll(bookingTasks);
        allTasks.addAll(advanceTasks);

        List<Future<Integer>> futures =
                executorService.invokeAll(allTasks);

        List<Integer> bookingTokens =
                new ArrayList<>();

        for (int i = 0; i < bookingTasks.size(); i++) {
            bookingTokens.add(futures.get(i).get());
        }

        List<Integer> successfulAdvanceTokens =
                new ArrayList<>();

        for (int i = bookingTasks.size();
             i < futures.size();
             i++) {

            int token = futures.get(i).get();

            if (token != -1) {
                successfulAdvanceTokens.add(token);
            }
        }

        // 10 concurrent bookings must receive
        // unique tokens 2 through 11.
        assertEquals(10, bookingTokens.size());
        assertEquals(
                10,
                bookingTokens.stream().distinct().count()
        );

        bookingTokens.sort(Integer::compareTo);

        assertEquals(
                List.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                bookingTokens
        );

        // At least the initial appointment must have
        // been successfully served.
        assertEquals(
                true,
                successfulAdvanceTokens.size() >= 1
        );

        // Successful advances must themselves be sequential.
        successfulAdvanceTokens.sort(Integer::compareTo);

        for (int i = 0;
             i < successfulAdvanceTokens.size();
             i++) {

            assertEquals(
                    i + 1,
                    successfulAdvanceTokens.get(i)
            );
        }

        Queue finalQueue =
                queueRepository.findById(queue.getId())
                        .orElseThrow();

        // 1 initial appointment + 10 concurrent bookings.
        assertEquals(11, finalQueue.getNextTokenNumber());

        // currentToken must match the last successfully
        // served appointment.
        assertEquals(
                successfulAdvanceTokens.get(
                        successfulAdvanceTokens.size() - 1
                ),
                finalQueue.getCurrentToken()
        );

        List<Appointment> appointments =
                appointmentRepository
                        .findByQueueIdOrderByTokenNumberAsc(
                                queue.getId()
                        );

        assertEquals(11, appointments.size());

        for (int i = 0; i < appointments.size(); i++) {
            Appointment appointment = appointments.get(i);

            assertEquals(
                    i + 1,
                    appointment.getTokenNumber()
            );

            assertTrue(
                    appointment.getStatus()
                            == AppointmentStatus.WAITING
                    || appointment.getStatus()
                            == AppointmentStatus.CALLED
            );
        }

        long calledCount =
                appointments.stream()
                        .filter(appointment ->
                                appointment.getStatus()
                                        == AppointmentStatus.CALLED)
                        .count();

        assertEquals(
                successfulAdvanceTokens.size(),
                calledCount
        );

    } finally {
        executorService.shutdown();
    }
}

    @Test
void shouldKeepAppointmentAllocationSeparateFromQueueAdvancement()
        throws Exception {

    User customerOne = createCustomer(
            "appointment-counter-1@test.com"
    );

    User customerTwo = createCustomer(
            "appointment-counter-2@test.com"
    );

    Business business = createBusiness(
            "appointment-counter-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    // Initial state
    Queue initialQueue = queueRepository.findById(queue.getId())
            .orElseThrow();

    assertEquals(0, initialQueue.getCurrentToken());
    assertEquals(0, initialQueue.getNextTokenNumber());

    // First appointment should allocate token 1
    mockMvc.perform(
            createAppointmentPost(
                        customerOne.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenNumber").value(1));

    Queue afterFirstAppointment =
            queueRepository.findById(queue.getId())
                    .orElseThrow();

    assertEquals(0, afterFirstAppointment.getCurrentToken());
    assertEquals(1, afterFirstAppointment.getNextTokenNumber());

    // Advancing the queue should only change currentToken
    mockMvc.perform(
            post("/api/v1/queues/{queueId}/next", queue.getId())
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentToken").value(1));

    Queue afterQueueAdvance =
            queueRepository.findById(queue.getId())
                    .orElseThrow();

    assertEquals(1, afterQueueAdvance.getCurrentToken());
    assertEquals(1, afterQueueAdvance.getNextTokenNumber());

    // Second appointment should receive token 2
    mockMvc.perform(
            createAppointmentPost(
                        customerTwo.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenNumber").value(2));

    Queue finalQueue =
            queueRepository.findById(queue.getId())
                    .orElseThrow();

    assertEquals(1, finalQueue.getCurrentToken());
    assertEquals(2, finalQueue.getNextTokenNumber());
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
                createAppointmentPost(
                        999999L,
                        queue.getId()
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
                createAppointmentPost(
                        customer.getId(),
                        999999L
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
                createAppointmentPost(
                        customer.getId(),
                        queue.getId()
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
                createAppointmentPost(
                        customer.getId(),
                        queue.getId()
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
                            createAppointmentPost(
                                    customer.getId(),
                                    queue.getId()
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
            createAppointmentPost(
                        businessOwner.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isConflict());
}

@Test
void shouldCallWaitingAppointment() throws Exception {

    User customer = createCustomer("appointment-call@test.com");

    Business business = createBusiness("appointment-call-business@test.com");

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String response = mockMvc.perform(
            createAppointmentPost(
                    customer.getId(),
                    queue.getId()
            )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode appointmentJson = objectMapper.readTree(response);

    Long appointmentId = appointmentJson
            .get("id")
            .asLong();

    Integer tokenNumber = appointmentJson
            .get("tokenNumber")
            .asInt();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call", appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(appointmentId))
            .andExpect(jsonPath("$.status").value("CALLED"));

    Queue updatedQueue = queueRepository.findById(queue.getId())
            .orElseThrow();

    assertEquals(
            tokenNumber,
            updatedQueue.getCurrentToken()
    );
}

@Test
void shouldRejectCallingAlreadyCalledAppointment() throws Exception {
    User customer = createCustomer("appointment-double-call@test.com");
    Business business = createBusiness("appointment-double-call-business@test.com");
    Service service = createService(business);
    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String response = mockMvc.perform(
            createAppointmentPost(
                    customer.getId(),
                    queue.getId()
            )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long appointmentId = objectMapper
            .readTree(response)
            .get("id")
            .asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call", appointmentId)
    )
            .andExpect(status().isOk());

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call", appointmentId)
    )
            .andExpect(status().isConflict());
}

@Test
void shouldReturnNotFoundWhenCallingMissingAppointment() throws Exception {
    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call", 999999L)
    )
            .andExpect(status().isNotFound());
}
@Test
void shouldStartCalledAppointment() throws Exception {
    User customer = createCustomer(
            "appointment-start@test.com"
    );

    Business business = createBusiness(
            "appointment-start-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CALLED"));

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/start",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(appointmentId))
            .andExpect(jsonPath("$.status").value("IN_SERVICE"));
}
@Test
void shouldRejectStartingWaitingAppointment() throws Exception {
    User customer = createCustomer(
            "appointment-start-waiting@test.com"
    );

    Business business = createBusiness(
            "appointment-start-waiting-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/start",
                    appointmentId)
    )
            .andExpect(status().isConflict());
}
@Test
void shouldReturnNotFoundWhenStartingMissingAppointment()
        throws Exception {

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/start",
                    999999L)
    )
            .andExpect(status().isNotFound());
}

@Test
void shouldCompleteInServiceAppointment() throws Exception {
    User customer = createCustomer(
            "appointment-complete@test.com"
    );

    Business business = createBusiness(
            "appointment-complete-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    // WAITING -> CALLED
    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CALLED"));

    // CALLED -> IN_SERVICE
    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/start",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_SERVICE"));

    // IN_SERVICE -> COMPLETED
    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/complete",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(appointmentId))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
}

@Test
void shouldRejectCompletingWaitingAppointment() throws Exception {
    User customer = createCustomer(
            "appointment-complete-waiting@test.com"
    );

    Business business = createBusiness(
            "appointment-complete-waiting-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/complete",
                    appointmentId)
    )
            .andExpect(status().isConflict());
}

@Test
void shouldRejectCompletingCalledAppointment() throws Exception {
    User customer = createCustomer(
            "appointment-complete-called@test.com"
    );

    Business business = createBusiness(
            "appointment-complete-called-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CALLED"));

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/complete",
                    appointmentId)
    )
            .andExpect(status().isConflict());
}

@Test
void shouldRejectCompletingAlreadyCompletedAppointment()
        throws Exception {

    User customer = createCustomer(
            "appointment-complete-twice@test.com"
    );

    Business business = createBusiness(
            "appointment-complete-twice-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call",
                    appointmentId)
    )
            .andExpect(status().isOk());

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/start",
                    appointmentId)
    )
            .andExpect(status().isOk());

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/complete",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/complete",
                    appointmentId)
    )
            .andExpect(status().isConflict());
}

@Test
void shouldReturnNotFoundWhenCompletingMissingAppointment()
        throws Exception {

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/complete",
                    999999L)
    )
            .andExpect(status().isNotFound());
}

@Test
void shouldCancelWaitingAppointment() throws Exception {
    User customer = createCustomer(
            "appointment-cancel-waiting@test.com"
    );

    Business business = createBusiness(
            "appointment-cancel-waiting-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/cancel",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(appointmentId))
            .andExpect(jsonPath("$.status").value("CANCELLED"));
}

@Test
void shouldCancelCalledAppointment() throws Exception {
    User customer = createCustomer(
            "appointment-cancel-called@test.com"
    );

    Business business = createBusiness(
            "appointment-cancel-called-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CALLED"));

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/cancel",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
}

@Test
void shouldRejectCancellingInServiceAppointment()
        throws Exception {

    User customer = createCustomer(
            "appointment-cancel-service@test.com"
    );

    Business business = createBusiness(
            "appointment-cancel-service-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call",
                    appointmentId)
    ).andExpect(status().isOk());

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/start",
                    appointmentId)
    ).andExpect(status().isOk());

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/cancel",
                    appointmentId)
    )
            .andExpect(status().isConflict());
}

@Test
void shouldRejectCancellingCompletedAppointment()
        throws Exception {

    User customer = createCustomer(
            "appointment-cancel-completed@test.com"
    );

    Business business = createBusiness(
            "appointment-cancel-completed-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/call",
                    appointmentId)
    ).andExpect(status().isOk());

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/start",
                    appointmentId)
    ).andExpect(status().isOk());

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/complete",
                    appointmentId)
    ).andExpect(status().isOk());

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/cancel",
                    appointmentId)
    )
            .andExpect(status().isConflict());
}

@Test
void shouldMarkCalledAppointmentAsNoShow() throws Exception {

    User customer = createCustomer(
            "no-show-called-customer@test.com"
    );

    Business business = createBusiness(
            "no-show-called-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    // Create appointment
    MvcResult createResult = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenNumber").value(1))
            .andReturn();

    JsonNode createdAppointment =
            objectMapper.readTree(
                    createResult.getResponse()
                            .getContentAsString()
            );

    long appointmentId =
            createdAppointment.get("id").asLong();

    // Call appointment
    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/call",
                    appointmentId
            )
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CALLED"));

    // Mark as no-show
    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/no-show",
                    appointmentId
            )
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(appointmentId))
            .andExpect(jsonPath("$.status").value("NO_SHOW"));

    Appointment appointment =
            appointmentRepository.findById(appointmentId)
                    .orElseThrow();

    assertEquals(
            AppointmentStatus.NO_SHOW,
            appointment.getStatus()
    );
}

@Test
void shouldRejectMarkingWaitingAppointmentAsNoShow()
        throws Exception {

    User customer = createCustomer(
            "no-show-waiting-customer@test.com"
    );

    Business business = createBusiness(
            "no-show-waiting-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    MvcResult createResult = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andReturn();

    long appointmentId =
            objectMapper.readTree(
                    createResult.getResponse()
                            .getContentAsString()
            ).get("id").asLong();

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/no-show",
                    appointmentId
            )
    )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                    .value(org.hamcrest.Matchers.containsString(
                            "cannot be marked as NO_SHOW"
                    )));
}

@Test
void shouldRejectMarkingInServiceAppointmentAsNoShow()
        throws Exception {

    User customer = createCustomer(
            "no-show-in-service-customer@test.com"
    );

    Business business = createBusiness(
            "no-show-in-service-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    MvcResult createResult = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn();

    long appointmentId =
            objectMapper.readTree(
                    createResult.getResponse()
                            .getContentAsString()
            ).get("id").asLong();

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/call",
                    appointmentId
            )
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CALLED"));

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/start",
                    appointmentId
            )
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_SERVICE"));

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/no-show",
                    appointmentId
            )
    )
            .andExpect(status().isConflict());
}

@Test
void shouldRejectMarkingCompletedAppointmentAsNoShow()
        throws Exception {

    User customer = createCustomer(
            "no-show-completed-customer@test.com"
    );

    Business business = createBusiness(
            "no-show-completed-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    MvcResult createResult = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn();

    long appointmentId =
            objectMapper.readTree(
                    createResult.getResponse()
                            .getContentAsString()
            ).get("id").asLong();

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/call",
                    appointmentId
            )
    )
            .andExpect(status().isOk());

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/start",
                    appointmentId
            )
    )
            .andExpect(status().isOk());

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/complete",
                    appointmentId
            )
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/no-show",
                    appointmentId
            )
    )
            .andExpect(status().isConflict());
}

@Test
void shouldRejectMarkingCancelledAppointmentAsNoShow()
        throws Exception {

    User customer = createCustomer(
            "no-show-cancelled-customer@test.com"
    );

    Business business = createBusiness(
            "no-show-cancelled-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    MvcResult createResult = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn();

    long appointmentId =
            objectMapper.readTree(
                    createResult.getResponse()
                            .getContentAsString()
            ).get("id").asLong();

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/cancel",
                    appointmentId
            )
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/no-show",
                    appointmentId
            )
    )
            .andExpect(status().isConflict());
}

@Test
void shouldReturnNotFoundWhenMarkingMissingAppointmentAsNoShow()
        throws Exception {

    mockMvc.perform(
            post(
                    "/api/v1/appointments/{appointmentId}/no-show",
                    999999L
            )
    )
            .andExpect(status().isNotFound());
}

@Test
void shouldReturnNotFoundWhenCancellingMissingAppointment()
        throws Exception {

    mockMvc.perform(
            post("/api/v1/appointments/{appointmentId}/cancel",
                    999999L)
    )
            .andExpect(status().isNotFound());
}

@Test
void shouldGetAppointmentById() throws Exception {
    User customer = createCustomer(
            "appointment-get@test.com"
    );

    Business business = createBusiness(
            "appointment-get-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    String createResponse = mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode createJson = objectMapper.readTree(createResponse);

    Long appointmentId = createJson.get("id").asLong();

    mockMvc.perform(
            get("/api/v1/appointments/{appointmentId}",
                    appointmentId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(appointmentId))
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
            .andExpect(jsonPath("$.tokenNumber").value(1))
            .andExpect(jsonPath("$.status").value("WAITING"));
}

@Test
void shouldReturnNotFoundWhenGettingMissingAppointment()
        throws Exception {

    mockMvc.perform(
            get("/api/v1/appointments/{appointmentId}",
                    999999L)
    )
            .andExpect(status().isNotFound());
}

@Test
void shouldGetCustomerAppointments() throws Exception {
    User customer = createCustomer(
            "appointment-history@test.com"
    );

    Business business = createBusiness(
            "appointment-history-business@test.com"
    );

    Service service = createService(business);

    Queue queue = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    // First appointment
    mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenNumber").value(1));

    // Second appointment
    // This is allowed because appointments currently do not
    // restrict a customer from joining the same queue twice.
    mockMvc.perform(
            createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
    )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenNumber").value(2));

    mockMvc.perform(
            get(
                    "/api/v1/appointments/customers/{customerId}",
                    customer.getId()
            )
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(
                    jsonPath("$[0].customerId")
                            .value(customer.getId())
            )
            .andExpect(
                    jsonPath("$[1].customerId")
                            .value(customer.getId())
            );
}

@Test
void shouldReturnNotFoundWhenGettingCustomerAppointments()
        throws Exception {

    mockMvc.perform(
            get(
                    "/api/v1/appointments/customers/{customerId}",
                    999999L
            )
    )
            .andExpect(status().isNotFound());
}

@Test
void shouldRejectAppointmentHistoryForNonCustomerUser()
        throws Exception {

    Business business = createBusiness(
            "appointment-history-role-business@test.com"
    );

    Long ownerId = business.getOwner().getId();

    mockMvc.perform(
            get(
                    "/api/v1/appointments/customers/{customerId}",
                    ownerId
            )
    )
            .andExpect(status().isConflict());
}

    /*
     * ============================================================
     * QUEUE POSITION AND ETA
     * ============================================================
     */

    @Test
    void shouldReturnCorrectPositionAndEtaForWaitingAppointment()
            throws Exception {

        User customerOne = createCustomer(
                "position-customer-1@test.com"
        );

        User customerTwo = createCustomer(
                "position-customer-2@test.com"
        );

        User customerThree = createCustomer(
                "position-customer-3@test.com"
        );

        Business business = createBusiness(
                "position-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                createAppointmentPost(
                        customerOne.getId(),
                        queue.getId()
                )
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(1));

        mockMvc.perform(
                createAppointmentPost(
                        customerTwo.getId(),
                        queue.getId()
                )
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(2));

        String thirdAppointmentResponse =
                mockMvc.perform(
                        createAppointmentPost(
                        customerThree.getId(),
                        queue.getId()
                )
                )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Long thirdAppointmentId =
                objectMapper.readTree(thirdAppointmentResponse)
                        .get("id")
                        .asLong();

        mockMvc.perform(
                get("/api/v1/appointments/{appointmentId}/position",
                        thirdAppointmentId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId")
                        .value(thirdAppointmentId))
                .andExpect(jsonPath("$.tokenNumber")
                        .value(3))
                .andExpect(jsonPath("$.status")
                        .value("WAITING"))
                .andExpect(jsonPath("$.queuePosition")
                        .value(2))
                .andExpect(jsonPath("$.estimatedWaitMinutes")
                        .value(60));
    }


    @Test
    void shouldReturnZeroPositionAndEtaForFirstWaitingAppointment()
            throws Exception {

        User customer = createCustomer(
                "position-first-customer@test.com"
        );

        Business business = createBusiness(
                "position-first-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        String response =
                mockMvc.perform(
                        createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
                )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Long appointmentId =
                objectMapper.readTree(response)
                        .get("id")
                        .asLong();

        mockMvc.perform(
                get("/api/v1/appointments/{appointmentId}/position",
                        appointmentId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId")
                        .value(appointmentId))
                .andExpect(jsonPath("$.tokenNumber")
                        .value(1))
                .andExpect(jsonPath("$.status")
                        .value("WAITING"))
                .andExpect(jsonPath("$.queuePosition")
                        .value(0))
                .andExpect(jsonPath("$.estimatedWaitMinutes")
                        .value(0));
    }


    @Test
    void shouldReturnZeroPositionAndEtaForCalledAppointment()
            throws Exception {

        User customer = createCustomer(
                "position-called-customer@test.com"
        );

        Business business = createBusiness(
                "position-called-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        String response =
                mockMvc.perform(
                        createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
                )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Long appointmentId =
                objectMapper.readTree(response)
                        .get("id")
                        .asLong();

        mockMvc.perform(
                post("/api/v1/appointments/{appointmentId}/call",
                        appointmentId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("CALLED"));

        mockMvc.perform(
                get("/api/v1/appointments/{appointmentId}/position",
                        appointmentId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId")
                        .value(appointmentId))
                .andExpect(jsonPath("$.tokenNumber")
                        .value(1))
                .andExpect(jsonPath("$.status")
                        .value("CALLED"))
                .andExpect(jsonPath("$.queuePosition")
                        .value(0))
                .andExpect(jsonPath("$.estimatedWaitMinutes")
                        .value(0));
    }


    @Test
    void shouldReturnZeroPositionAndEtaForCancelledAppointment()
            throws Exception {

        User customer = createCustomer(
                "position-cancelled-customer@test.com"
        );

        Business business = createBusiness(
                "position-cancelled-business@test.com"
        );

        Service service = createService(business);

        Queue queue = createQueue(
                business,
                service,
                LocalDate.of(2026, 8, 31)
        );

        String response =
                mockMvc.perform(
                        createAppointmentPost(
                        customer.getId(),
                        queue.getId()
                )
                )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Long appointmentId =
                objectMapper.readTree(response)
                        .get("id")
                        .asLong();

        mockMvc.perform(
                post("/api/v1/appointments/{appointmentId}/cancel",
                        appointmentId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("CANCELLED"));

        mockMvc.perform(
                get("/api/v1/appointments/{appointmentId}/position",
                        appointmentId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId")
                        .value(appointmentId))
                .andExpect(jsonPath("$.tokenNumber")
                        .value(1))
                .andExpect(jsonPath("$.status")
                        .value("CANCELLED"))
                .andExpect(jsonPath("$.queuePosition")
                        .value(0))
                .andExpect(jsonPath("$.estimatedWaitMinutes")
                        .value(0));
    }


    @Test
    void shouldReturnNotFoundForMissingAppointmentPosition()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/appointments/{appointmentId}/position",
                        999999L)
        )
                .andExpect(status().isNotFound());
    }
}