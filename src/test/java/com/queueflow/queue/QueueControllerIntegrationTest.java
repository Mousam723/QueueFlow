package com.queueflow.queue;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.business.BusinessStatus;
import com.queueflow.queue.dto.CreateQueueRequest;
import com.queueflow.queue.QueueRepository;
import com.queueflow.service.Service;
import com.queueflow.service.ServiceRepository;
import com.queueflow.service.ServiceStatus;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;
import com.queueflow.appointment.Appointment;
import com.queueflow.appointment.AppointmentRepository;
import com.queueflow.appointment.AppointmentStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.hasSize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
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
        business.setDescription(
                "Business created for queue integration tests"
        );
        business.setAddress("Hyderabad");
        business.setTimezone("Asia/Kolkata");
        business.setStatus(BusinessStatus.ACTIVE);

        return businessRepository.save(business);
    }

    private User createCustomer(String email) {
        User customer = new User();
        customer.setName("Queue Test Customer");
        customer.setEmail(email);
        customer.setPhone("8888888888");
        customer.setPasswordHash("test-password");
        customer.setRole(UserRole.CUSTOMER);
        return userRepository.save(customer);
    }

    private Service createService(Business business) {

        Service service = new Service();

        service.setBusiness(business);
        service.setName("Test Service");
        service.setDescription(
                "Service created for queue integration tests"
        );
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

        JsonNode jsonNode =
                objectMapper.readTree(response);

        Long queueId =
                jsonNode.get("id").asLong();

        assertNotNull(queueId);

        return queueId;
    }

    private Appointment createAppointment(
        Business business,
        Service service,
        Long queueId,
        User customer,
        int tokenNumber
) {
    Queue queue = queueRepository.findById(queueId)
            .orElseThrow();

    Appointment appointment = new Appointment();
    appointment.setIdempotencyKey(
        "test-" + java.util.UUID.randomUUID()
);
    appointment.setRequestFingerprint(
        java.util.UUID.randomUUID().toString()
    );
    appointment.setCustomer(customer);
    appointment.setBusiness(business);
    appointment.setService(service);
    appointment.setQueue(queue);
    appointment.setTokenNumber(tokenNumber);
    appointment.setAppointmentDate(queue.getQueueDate());
    appointment.setStatus(AppointmentStatus.WAITING);

    return appointmentRepository.save(appointment);
}

    /*
     * ============================================================
     * QUEUE CREATION
     * ============================================================
     */

    @Test
    void shouldCreateQueueForBusinessAndService()
            throws Exception {

        Business business = createBusiness(
                "queue-create@test.com"
        );

        Service service = createService(business);

        CreateQueueRequest request =
                new CreateQueueRequest();

        request.setServiceId(service.getId());
        request.setQueueDate(
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
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
                .andExpect(jsonPath("$.id").exists())
                .andExpect(
                        jsonPath("$.businessId")
                                .value(business.getId())
                )
                .andExpect(
                        jsonPath("$.serviceId")
                                .value(service.getId())
                )
                .andExpect(
                        jsonPath("$.queueDate")
                                .value("2026-08-31")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.currentToken")
                                .value(0)
                );
    }


    @Test
    void shouldDefaultQueueDateToToday()
            throws Exception {

        Business business = createBusiness(
                "queue-default-date@test.com"
        );

        Service service = createService(business);

        CreateQueueRequest request =
                new CreateQueueRequest();

        request.setServiceId(service.getId());

        mockMvc.perform(
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
                .andExpect(
                        jsonPath("$.queueDate")
                                .value(
                                        LocalDate.now().toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.currentToken")
                                .value(0)
                );
    }


    @Test
    void shouldRejectQueueForNonExistentBusiness()
            throws Exception {

        CreateQueueRequest request =
                new CreateQueueRequest();

        request.setServiceId(999999L);

        request.setQueueDate(
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post(
                        "/api/v1/businesses/{businessId}/queues",
                        999999L
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldRejectQueueForNonExistentService()
            throws Exception {

        Business business = createBusiness(
                "queue-invalid-service@test.com"
        );

        CreateQueueRequest request =
                new CreateQueueRequest();

        request.setServiceId(999999L);

        request.setQueueDate(
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post(
                        "/api/v1/businesses/{businessId}/queues",
                        business.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldRejectServiceThatDoesNotBelongToBusiness()
            throws Exception {

        Business businessOne = createBusiness(
                "queue-business-one@test.com"
        );

        Business businessTwo = createBusiness(
                "queue-business-two@test.com"
        );

        Service service =
                createService(businessOne);

        CreateQueueRequest request =
                new CreateQueueRequest();

        request.setServiceId(service.getId());

        request.setQueueDate(
                LocalDate.of(2026, 8, 31)
        );

        mockMvc.perform(
                post(
                        "/api/v1/businesses/{businessId}/queues",
                        businessTwo.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldRejectDuplicateQueueForSameServiceAndDate()
            throws Exception {

        Business business = createBusiness(
                "queue-duplicate@test.com"
        );

        Service service =
                createService(business);

        LocalDate queueDate =
                LocalDate.of(2026, 8, 31);

        createQueue(
                business,
                service,
                queueDate
        );

        CreateQueueRequest duplicateRequest =
                new CreateQueueRequest();

        duplicateRequest.setServiceId(
                service.getId()
        );

        duplicateRequest.setQueueDate(
                queueDate
        );

        mockMvc.perform(
                post(
                        "/api/v1/businesses/{businessId}/queues",
                        business.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        duplicateRequest
                                )
                        )
        )
                .andExpect(status().isConflict());
    }


    /*
     * ============================================================
     * QUEUE RETRIEVAL
     * ============================================================
     */

    @Test
    void shouldGetQueueById()
            throws Exception {

        Business business = createBusiness(
                "queue-get@test.com"
        );

        Service service =
                createService(business);

        Long queueId =
                createQueue(
                        business,
                        service,
                        LocalDate.of(2026, 8, 31)
                );

        mockMvc.perform(
                get(
                        "/api/v1/queues/{queueId}",
                        queueId
                )
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(queueId)
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
                        jsonPath("$.queueDate")
                                .value("2026-08-31")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.currentToken")
                                .value(0)
                );
    }


    @Test
    void shouldReturn404ForNonExistentQueue()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/queues/{queueId}",
                        999999L
                )
        )
                .andExpect(status().isNotFound());
    }


    /*
     * ============================================================
     * BUSINESS QUEUE LISTING
     * ============================================================
     */

    @Test
    void shouldListQueuesForBusinessAndDate()
            throws Exception {

        Business business = createBusiness(
                "queue-list@test.com"
        );

        Service serviceOne =
                createService(business);

        Service serviceTwo =
                createService(business);

        LocalDate queueDate =
                LocalDate.of(2026, 8, 31);

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
                get(
                        "/api/v1/businesses/{businessId}/queues",
                        business.getId()
                )
                        .param(
                                "date",
                                queueDate.toString()
                        )
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$", hasSize(2))
                );
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

    User customer = createCustomer(
            "queue-next-customer@test.com"
    );

    Appointment appointment = createAppointment(
            business,
            service,
            queueId,
            customer,
            1
    );

    mockMvc.perform(
            post(
                    "/api/v1/queues/{queueId}/next",
                    queueId
            )
    )
            .andExpect(status().isOk())
            .andExpect(
                    jsonPath("$.id").value(queueId)
            )
            .andExpect(
                    jsonPath("$.status").value("ACTIVE")
            )
            .andExpect(
                    jsonPath("$.currentToken").value(1)
            );

    Appointment updatedAppointment =
            appointmentRepository.findById(appointment.getId())
                    .orElseThrow();

    assertEquals(
            AppointmentStatus.CALLED,
            updatedAppointment.getStatus()
    );
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

    User customer1 = createCustomer(
            "queue-next-multiple-1@test.com"
    );

    User customer2 = createCustomer(
            "queue-next-multiple-2@test.com"
    );

    User customer3 = createCustomer(
            "queue-next-multiple-3@test.com"
    );

    Appointment appointment1 = createAppointment(
            business,
            service,
            queueId,
            customer1,
            1
    );

    Appointment appointment2 = createAppointment(
            business,
            service,
            queueId,
            customer2,
            2
    );

    Appointment appointment3 = createAppointment(
            business,
            service,
            queueId,
            customer3,
            3
    );

    mockMvc.perform(
            post(
                    "/api/v1/queues/{queueId}/next",
                    queueId
            )
    )
            .andExpect(status().isOk())
            .andExpect(
                    jsonPath("$.currentToken").value(1)
            );

    mockMvc.perform(
            post(
                    "/api/v1/queues/{queueId}/next",
                    queueId
            )
    )
            .andExpect(status().isOk())
            .andExpect(
                    jsonPath("$.currentToken").value(2)
            );

    mockMvc.perform(
            post(
                    "/api/v1/queues/{queueId}/next",
                    queueId
            )
    )
            .andExpect(status().isOk())
            .andExpect(
                    jsonPath("$.currentToken").value(3)
            );

    assertEquals(
            AppointmentStatus.CALLED,
            appointmentRepository.findById(appointment1.getId())
                    .orElseThrow()
                    .getStatus()
    );

    assertEquals(
            AppointmentStatus.CALLED,
            appointmentRepository.findById(appointment2.getId())
                    .orElseThrow()
                    .getStatus()
    );

    assertEquals(
            AppointmentStatus.CALLED,
            appointmentRepository.findById(appointment3.getId())
                    .orElseThrow()
                    .getStatus()
    );
}


    @Test
    void shouldReturn404WhenAdvancingNonExistentQueue()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/queues/{queueId}/next",
                        999999L
                )
        )
                .andExpect(status().isNotFound());
    }


    /*
     * ============================================================
     * CONCURRENT TOKEN ADVANCEMENT
     * ============================================================
     */

    @Test
void shouldSafelyAdvanceQueueWithConcurrentRequests()
        throws Exception {

    Business business = createBusiness(
            "queue-concurrency@test.com"
    );

    Service service = createService(business);

    Long queueId = createQueue(
            business,
            service,
            LocalDate.of(2026, 8, 31)
    );

    int requestCount = 10;

    List<Appointment> appointments = new ArrayList<>();

    for (int i = 1; i <= requestCount; i++) {
        User customer = createCustomer(
                "queue-concurrency-" + i + "@test.com"
        );

        appointments.add(
                createAppointment(
                        business,
                        service,
                        queueId,
                        customer,
                        i
                )
        );
    }

    ExecutorService executorService =
            Executors.newFixedThreadPool(requestCount);

    try {
        List<Callable<Integer>> tasks =
                new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            tasks.add(() -> {
                String response =
                        mockMvc.perform(
                                post(
                                        "/api/v1/queues/{queueId}/next",
                                        queueId
                                )
                        )
                                .andExpect(
                                        status().isOk()
                                )
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode jsonNode =
                        objectMapper.readTree(response);

                return jsonNode
                        .get("currentToken")
                        .asInt();
            });
        }

        List<Future<Integer>> results =
                executorService.invokeAll(tasks);

        List<Integer> tokens =
                new ArrayList<>();

        for (Future<Integer> result : results) {
            tokens.add(result.get());
        }

        assertEquals(
                requestCount,
                tokens.size()
        );

        assertEquals(
                requestCount,
                tokens.stream()
                        .distinct()
                        .count()
        );

        assertEquals(
                requestCount,
                tokens.stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0)
        );

        for (Appointment appointment : appointments) {
            Appointment updatedAppointment =
                    appointmentRepository
                            .findById(appointment.getId())
                            .orElseThrow();

            assertEquals(
                    AppointmentStatus.CALLED,
                    updatedAppointment.getStatus()
            );
        }

        mockMvc.perform(
                get(
                        "/api/v1/queues/{queueId}",
                        queueId
                )
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.currentToken")
                                .value(requestCount)
                );

    } finally {
        executorService.shutdown();
    }
}

    @Test
void shouldPauseActiveQueue() throws Exception {
    Business business = createBusiness("queue-pause@test.com");
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
            .andExpect(jsonPath("$.status").value("PAUSED"))
            .andExpect(jsonPath("$.currentToken").value(0));
}

@Test
void shouldResumePausedQueue() throws Exception {
    Business business = createBusiness("queue-resume@test.com");
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
            post("/api/v1/queues/{queueId}/resume", queueId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
}

@Test
void shouldCloseActiveQueue() throws Exception {
    Business business = createBusiness("queue-close@test.com");
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
            .andExpect(jsonPath("$.status").value("CLOSED"));
}

@Test
void shouldRejectAdvancingPausedQueue() throws Exception {
    Business business = createBusiness("queue-paused-next@test.com");
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
void shouldRejectAdvancingClosedQueue() throws Exception {
    Business business = createBusiness("queue-closed-next@test.com");
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
}

@Test
void shouldRejectPausingAlreadyPausedQueue() throws Exception {
    Business business = createBusiness("queue-double-pause@test.com");
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
void shouldRejectResumingActiveQueue() throws Exception {
    Business business = createBusiness("queue-active-resume@test.com");
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
void shouldRejectResumingClosedQueue() throws Exception {
    Business business = createBusiness("queue-closed-resume@test.com");
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
            post("/api/v1/queues/{queueId}/resume", queueId)
    )
            .andExpect(status().isConflict());
}

@Test
void shouldRejectClosingAlreadyClosedQueue() throws Exception {
    Business business = createBusiness("queue-double-close@test.com");
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
            post("/api/v1/queues/{queueId}/close", queueId)
    )
            .andExpect(status().isConflict());
}
}
