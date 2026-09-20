package com.queueflow.auth;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.business.BusinessStatus;
import com.queueflow.business.dto.CreateBusinessRequest;
import com.queueflow.businessstaff.BusinessStaff;
import com.queueflow.businessstaff.BusinessStaffRepository;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;
import com.queueflow.service.dto.CreateServiceRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "queueflow.security.enabled=true")
@AutoConfigureMockMvc
@Transactional
class AuthorizationIntegrationTest {

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
    private JwtService jwtService;

    private User admin;
    private User owner;
    private User anotherOwner;
    private User staff;
    private User unassignedStaff;

    private String adminToken;
    private String ownerToken;
    private String anotherOwnerToken;
    private String staffToken;
    private String unassignedStaffToken;

    private Business ownerBusiness;
    private Business anotherOwnerBusiness;

    @BeforeEach
    void setUp() {

        admin = createUser(
                "Authorization Admin",
                "authorization-admin@test.com",
                UserRole.ADMIN
        );

        owner = createUser(
                "Authorization Owner",
                "authorization-owner@test.com",
                UserRole.BUSINESS_OWNER
        );

        anotherOwner = createUser(
                "Another Owner",
                "another-owner@test.com",
                UserRole.BUSINESS_OWNER
        );

        staff = createUser(
                "Assigned Staff",
                "assigned-staff@test.com",
                UserRole.STAFF
        );

        unassignedStaff = createUser(
                "Unassigned Staff",
                "unassigned-staff@test.com",
                UserRole.STAFF
        );

        ownerBusiness = createBusiness(
                owner,
                "Owner Business"
        );

        anotherOwnerBusiness = createBusiness(
                anotherOwner,
                "Another Owner Business"
        );

        BusinessStaff assignment = new BusinessStaff();
        assignment.setBusiness(ownerBusiness);
        assignment.setStaff(staff);
        businessStaffRepository.save(assignment);

        adminToken = jwtService.generateToken(
            admin.getId(),
            admin.getEmail(),
            admin.getRole().name()
    );
    
    ownerToken = jwtService.generateToken(
            owner.getId(),
            owner.getEmail(),
            owner.getRole().name()
    );
    
    anotherOwnerToken = jwtService.generateToken(
            anotherOwner.getId(),
            anotherOwner.getEmail(),
            anotherOwner.getRole().name()
    );
    
    staffToken = jwtService.generateToken(
            staff.getId(),
            staff.getEmail(),
            staff.getRole().name()
    );
    
    unassignedStaffToken = jwtService.generateToken(
            unassignedStaff.getId(),
            unassignedStaff.getEmail(),
            unassignedStaff.getRole().name()
    );
    }
    @Test
    void shouldAllowBusinessOwnerToCreateOwnBusiness() throws Exception {
        CreateBusinessRequest request = new CreateBusinessRequest();
        request.setOwnerId(owner.getId());
        request.setName("New Owner Business");
        request.setDescription("Created by owner");
        request.setAddress("Test Address");
        request.setTimezone("Asia/Kolkata");
    
        mockMvc.perform(
                post("/api/v1/businesses")
                        .header(
                                "Authorization",
                                "Bearer " + ownerToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectBusinessOwnerFromCreatingBusinessForAnotherOwner()
            throws Exception {
    
        CreateBusinessRequest request = new CreateBusinessRequest();
        request.setOwnerId(anotherOwner.getId());
        request.setName("Unauthorized Business");
        request.setDescription("Should not be created");
        request.setAddress("Test Address");
        request.setTimezone("Asia/Kolkata");
    
        mockMvc.perform(
                post("/api/v1/businesses")
                        .header(
                                "Authorization",
                                "Bearer " + ownerToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectStaffFromCreatingBusiness() throws Exception {
        CreateBusinessRequest request = new CreateBusinessRequest();
        request.setOwnerId(staff.getId());
        request.setName("Staff Business");
        request.setDescription("Should not be created");
        request.setAddress("Test Address");
        request.setTimezone("Asia/Kolkata");
    
        mockMvc.perform(
                post("/api/v1/businesses")
                        .header(
                                "Authorization",
                                "Bearer " + staffToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToCreateBusinessForAnotherOwner()
            throws Exception {
    
        CreateBusinessRequest request = new CreateBusinessRequest();
        request.setOwnerId(anotherOwner.getId());
        request.setName("Admin Created Business");
        request.setDescription("Created by admin");
        request.setAddress("Test Address");
        request.setTimezone("Asia/Kolkata");
    
        mockMvc.perform(
                post("/api/v1/businesses")
                        .header(
                                "Authorization",
                                "Bearer " + adminToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn401WhenAccessingBusinessWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}",
                        ownerBusiness.getId())
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowBusinessOwnerToListOwnBusinesses()
            throws Exception {
    
        mockMvc.perform(
                get("/api/v1/businesses")
                        .param("ownerId", owner.getId().toString())
                        .header(
                                "Authorization",
                                "Bearer " + ownerToken
                        )
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectBusinessOwnerFromListingAnotherOwnersBusinesses()
            throws Exception {
    
        mockMvc.perform(
                get("/api/v1/businesses")
                        .param("ownerId", anotherOwner.getId().toString())
                        .header(
                                "Authorization",
                                "Bearer " + ownerToken
                        )
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToListAnotherOwnersBusinesses()
            throws Exception {
    
        mockMvc.perform(
                get("/api/v1/businesses")
                        .param("ownerId", anotherOwner.getId().toString())
                        .header(
                                "Authorization",
                                "Bearer " + adminToken
                        )
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectStaffFromListingBusinesses()
            throws Exception {
    
        mockMvc.perform(
                get("/api/v1/businesses")
                        .param("ownerId", owner.getId().toString())
                        .header(
                                "Authorization",
                                "Bearer " + staffToken
                        )
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowBusinessOwnerToAccessOwnBusiness()
            throws Exception {

                mockMvc.perform(
                    get("/api/v1/businesses/" + ownerBusiness.getId())
                            .header(
                                    "Authorization",
                                    "Bearer " + ownerToken
                            )
            )
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectBusinessOwnerAccessToAnotherBusiness()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}",
                        anotherOwnerBusiness.getId())
                        .header(
                                "Authorization",
                                bearerToken(owner)
                        )
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToAccessAnyBusiness()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}",
                        anotherOwnerBusiness.getId())
                        .header(
                                "Authorization",
                                bearerToken(admin)
                        )
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAssignedStaffToAccessBusiness()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}",
                        ownerBusiness.getId())
                        .header(
                                "Authorization",
                                bearerToken(staff)
                        )
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnassignedStaffFromBusiness()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}",
                        ownerBusiness.getId())
                        .header(
                                "Authorization",
                                bearerToken(unassignedStaff)
                        )
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowBusinessOwnerToDeactivateOwnBusiness()
            throws Exception {

        mockMvc.perform(
                patch("/api/v1/businesses/{businessId}/deactivate",
                        ownerBusiness.getId())
                        .header(
                                "Authorization",
                                bearerToken(owner)
                        )
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAnotherOwnerFromDeactivatingBusiness()
            throws Exception {

        mockMvc.perform(
                patch("/api/v1/businesses/{businessId}/deactivate",
                        ownerBusiness.getId())
                        .header(
                                "Authorization",
                                bearerToken(anotherOwner)
                        )
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectStaffFromDeactivatingBusiness()
            throws Exception {

        mockMvc.perform(
                patch("/api/v1/businesses/{businessId}/deactivate",
                        ownerBusiness.getId())
                        .header(
                                "Authorization",
                                bearerToken(staff)
                        )
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToDeactivateBusiness()
            throws Exception {

        mockMvc.perform(
                patch("/api/v1/businesses/{businessId}/deactivate",
                        ownerBusiness.getId())
                        .header(
                                "Authorization",
                                bearerToken(admin)
                        )
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowBusinessOwnerToAssignStaff() throws Exception {
        mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/staff",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "staffId": %d
                                        }
                                        """.formatted(unassignedStaff.getId()))
                )
                .andExpect(status().isCreated());
    }
    
    @Test
    void shouldAllowAdminToAssignStaffToAnyBusiness() throws Exception {
        mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/staff",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "staffId": %d
                                        }
                                        """.formatted(unassignedStaff.getId()))
                )
                .andExpect(status().isCreated());
    }
    
    @Test
    void shouldRejectAnotherBusinessOwnerFromAssigningStaff() throws Exception {
        mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/staff",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + anotherOwnerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "staffId": %d
                                        }
                                        """.formatted(unassignedStaff.getId()))
                )
                .andExpect(status().isForbidden());
    }
    
    @Test
    void shouldRejectStaffFromAssigningStaff() throws Exception {
        mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/staff",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + staffToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "staffId": %d
                                        }
                                        """.formatted(unassignedStaff.getId()))
                )
                .andExpect(status().isForbidden());
    }
    
    @Test
    void shouldAllowBusinessOwnerToListBusinessStaff() throws Exception {
        mockMvc.perform(
                        get("/api/v1/businesses/{businessId}/staff",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                )
                .andExpect(status().isOk());
    }
    
    @Test
    void shouldAllowAdminToListAnyBusinessStaff() throws Exception {
        mockMvc.perform(
                        get("/api/v1/businesses/{businessId}/staff",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk());
    }
    
    @Test
    void shouldRejectAnotherBusinessOwnerFromListingStaff() throws Exception {
        mockMvc.perform(
                        get("/api/v1/businesses/{businessId}/staff",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + anotherOwnerToken
                                )
                )
                .andExpect(status().isForbidden());
    }
    
    @Test
    void shouldRejectStaffFromListingBusinessStaff() throws Exception {
        mockMvc.perform(
                        get("/api/v1/businesses/{businessId}/staff",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + staffToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowBusinessOwnerToCreateService() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("Owner Service");
        request.setDescription("Created by business owner");
        request.setDurationMinutes(30);
    
        mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/services",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());
    }
    
    @Test
    void shouldAllowAdminToCreateServiceForAnyBusiness() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("Admin Service");
        request.setDescription("Created by admin");
        request.setDurationMinutes(30);
    
        mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/services",
                                anotherOwnerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());
    }
    
    @Test
    void shouldRejectAnotherBusinessOwnerFromCreatingService() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("Unauthorized Service");
        request.setDescription("Should not be created");
        request.setDurationMinutes(30);
    
        mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/services",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + anotherOwnerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }
    
    @Test
    void shouldRejectAssignedStaffFromCreatingService() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("Staff Service");
        request.setDescription("Should not be created");
        request.setDurationMinutes(30);
    
        mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/services",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + staffToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }
    
    @Test
    void shouldRejectUnassignedStaffFromCreatingService() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("Unassigned Staff Service");
        request.setDescription("Should not be created");
        request.setDurationMinutes(30);
    
        mockMvc.perform(
                        post("/api/v1/businesses/{businessId}/services",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + unassignedStaffToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }
    
    @Test
    void shouldAllowBusinessOwnerToListServices() throws Exception {
        mockMvc.perform(
                        get("/api/v1/businesses/{businessId}/services",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                )
                .andExpect(status().isOk());
    }
    
    @Test
    void shouldAllowAdminToListServicesForAnyBusiness() throws Exception {
        mockMvc.perform(
                        get("/api/v1/businesses/{businessId}/services",
                                anotherOwnerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk());
    }
    
    @Test
    void shouldAllowAssignedStaffToListServices() throws Exception {
        mockMvc.perform(
                        get("/api/v1/businesses/{businessId}/services",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + staffToken
                                )
                )
                .andExpect(status().isOk());
    }
    
    @Test
    void shouldRejectUnassignedStaffFromListingServices() throws Exception {
        mockMvc.perform(
                        get("/api/v1/businesses/{businessId}/services",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + unassignedStaffToken
                                )
                )
                .andExpect(status().isForbidden());
    }
    
    @Test
    void shouldRejectAnotherBusinessOwnerFromListingServices() throws Exception {
        mockMvc.perform(
                        get("/api/v1/businesses/{businessId}/services",
                                ownerBusiness.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + anotherOwnerToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidJwt()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/businesses/{businessId}",
                        ownerBusiness.getId())
                        .header(
                                "Authorization",
                                "Bearer invalid-token"
                        )
        )
                .andExpect(status().isUnauthorized());
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
        user.setPasswordHash("test-password");
        user.setRole(role);

        return userRepository.save(user);
    }

    private Business createBusiness(
            User owner,
            String name
    ) {
        Business business = new Business();
        business.setOwner(owner);
        business.setName(name);
        business.setDescription("Authorization test business");
        business.setAddress("Test Address");
        business.setTimezone("Asia/Kolkata");
        business.setStatus(BusinessStatus.ACTIVE);

        return businessRepository.save(business);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}