package com.queueflow.customer;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.business.BusinessStatus;
import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueRepository;
import com.queueflow.queue.QueueStatus;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "CUSTOMER")
@Transactional
class CustomerDiscoveryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private QueueRepository queueRepository;

    private Business createBusiness(
            String name,
            BusinessStatus status
    ) {
        User owner = new User();
        owner.setName("Discovery Test Owner");
        owner.setEmail(
                "discovery-owner-" + UUID.randomUUID() + "@test.com"
        );
        owner.setPhone("9999999999");
        owner.setPasswordHash("test-password");
        owner.setRole(UserRole.BUSINESS_OWNER);

        owner = userRepository.save(owner);

        Business business = new Business();
        business.setOwner(owner);
        business.setName(name);
        business.setDescription("Discovery test business");
        business.setAddress("Raipur");
        business.setTimezone("Asia/Kolkata");
        business.setStatus(status);

        return businessRepository.save(business);
    }

    private Service createService(
            Business business,
            String name,
            ServiceStatus status
    ) {
        Service service = new Service();
        service.setBusiness(business);
        service.setName(name);
        service.setDescription("Discovery test service");
        service.setDurationMinutes(30);
        service.setStatus(status);

        return serviceRepository.save(service);
    }

    private Queue createQueue(
            Business business,
            Service service,
            LocalDate queueDate,
            QueueStatus status
    ) {
        Queue queue = new Queue();
        queue.setBusiness(business);
        queue.setService(service);
        queue.setQueueDate(queueDate);
        queue.setStatus(status);
        queue.setCurrentToken(0);
        queue.setNextTokenNumber(1);

        return queueRepository.save(queue);
    }

    @Test
    void shouldReturnOnlyActiveBusinesses() throws Exception {
        Business activeBusiness = createBusiness(
                "Active Clinic",
                BusinessStatus.ACTIVE
        );

        createBusiness(
                "Inactive Clinic",
                BusinessStatus.INACTIVE
        );

        mockMvc.perform(
                get("/api/v1/customer/businesses")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id")
                .value(activeBusiness.getId()))
        .andExpect(jsonPath("$.content[0].name")
                .value("Active Clinic"))
        .andExpect(jsonPath("$.content[0].status")
                .value("ACTIVE"))
        .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldPaginateActiveBusinesses() throws Exception {
        createBusiness("Business 1", BusinessStatus.ACTIVE);
        createBusiness("Business 2", BusinessStatus.ACTIVE);
        createBusiness("Business 3", BusinessStatus.ACTIVE);

        mockMvc.perform(
                get("/api/v1/customer/businesses")
                        .param("page", "0")
                        .param("size", "2")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void shouldReturnOnlyActiveServicesForActiveBusiness()
            throws Exception {

        Business business = createBusiness(
                "Active Clinic",
                BusinessStatus.ACTIVE
        );

        Service activeService = createService(
                business,
                "General Consultation",
                ServiceStatus.ACTIVE
        );

        createService(
                business,
                "Inactive Consultation",
                ServiceStatus.INACTIVE
        );

        mockMvc.perform(
                get(
                        "/api/v1/customer/businesses/{businessId}/services",
                        business.getId()
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id")
                .value(activeService.getId()))
        .andExpect(jsonPath("$.content[0].name")
                .value("General Consultation"))
        .andExpect(jsonPath("$.content[0].status")
                .value("ACTIVE"))
        .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn404ForInactiveBusinessServices()
            throws Exception {

        Business inactiveBusiness = createBusiness(
                "Inactive Clinic",
                BusinessStatus.INACTIVE
        );

        mockMvc.perform(
                get(
                        "/api/v1/customer/businesses/{businessId}/services",
                        inactiveBusiness.getId()
                )
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldReturnActiveQueueForBusinessAndService()
            throws Exception {

        Business business = createBusiness(
                "Active Clinic",
                BusinessStatus.ACTIVE
        );

        Service service = createService(
                business,
                "General Consultation",
                ServiceStatus.ACTIVE
        );

        LocalDate queueDate = LocalDate.of(2026, 9, 25);

        Queue queue = createQueue(
                business,
                service,
                queueDate,
                QueueStatus.ACTIVE
        );

        assertNotNull(queue.getId());

        mockMvc.perform(
                get(
                        "/api/v1/customer/businesses/{businessId}/services/{serviceId}/queues",
                        business.getId(),
                        service.getId()
                )
                .param("date", queueDate.toString())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(queue.getId()))
        .andExpect(jsonPath("$.businessId")
                .value(business.getId()))
        .andExpect(jsonPath("$.serviceId")
                .value(service.getId()))
        .andExpect(jsonPath("$.queueDate")
                .value(queueDate.toString()))
        .andExpect(jsonPath("$.status")
                .value("ACTIVE"))
        .andExpect(jsonPath("$.currentToken").value(0));
    }

    @Test
    void shouldReturn404WhenActiveQueueDoesNotExist()
            throws Exception {

        Business business = createBusiness(
                "Active Clinic",
                BusinessStatus.ACTIVE
        );

        Service service = createService(
                business,
                "General Consultation",
                ServiceStatus.ACTIVE
        );

        LocalDate queueDate = LocalDate.of(2026, 9, 25);

        mockMvc.perform(
                get(
                        "/api/v1/customer/businesses/{businessId}/services/{serviceId}/queues",
                        business.getId(),
                        service.getId()
                )
                .param("date", queueDate.toString())
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRejectServiceFromDifferentBusiness()
            throws Exception {

        Business firstBusiness = createBusiness(
                "First Clinic",
                BusinessStatus.ACTIVE
        );

        Business secondBusiness = createBusiness(
                "Second Clinic",
                BusinessStatus.ACTIVE
        );

        Service service = createService(
                secondBusiness,
                "Second Clinic Service",
                ServiceStatus.ACTIVE
        );

        mockMvc.perform(
                get(
                        "/api/v1/customer/businesses/{businessId}/services/{serviceId}/queues",
                        firstBusiness.getId(),
                        service.getId()
                )
                .param("date", "2026-09-25")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message")
                .value(
                        "Service does not belong to business with id "
                                + firstBusiness.getId()
                ));
    }

    @Test
    void shouldRejectInactiveServiceQueueLookup()
            throws Exception {

        Business business = createBusiness(
                "Active Clinic",
                BusinessStatus.ACTIVE
        );

        Service inactiveService = createService(
                business,
                "Inactive Service",
                ServiceStatus.INACTIVE
        );

        mockMvc.perform(
                get(
                        "/api/v1/customer/businesses/{businessId}/services/{serviceId}/queues",
                        business.getId(),
                        inactiveService.getId()
                )
                .param("date", "2026-09-25")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message")
                .value(
                        "Service with id "
                                + inactiveService.getId()
                                + " is not active"
                ));
    }

    @Test
    void shouldNotReturnInactiveQueue() throws Exception {
        Business business = createBusiness(
                "Active Clinic",
                BusinessStatus.ACTIVE
        );

        Service service = createService(
                business,
                "General Consultation",
                ServiceStatus.ACTIVE
        );

        LocalDate queueDate = LocalDate.of(2026, 9, 25);

        createQueue(
                business,
                service,
                queueDate,
                QueueStatus.CLOSED
        );

        mockMvc.perform(
                get(
                        "/api/v1/customer/businesses/{businessId}/services/{serviceId}/queues",
                        business.getId(),
                        service.getId()
                )
                .param("date", queueDate.toString())
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }
}
