package com.queueflow.business;

import tools.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
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

@Test
void shouldGetBusinessesByOwnerSuccessfully() throws Exception {

    User owner = new User();
    owner.setName("List Business Owner");
    owner.setEmail("list-business-owner@test.com");
    owner.setPhone("9999999998");
    owner.setPasswordHash("test-password");
    owner.setRole(UserRole.BUSINESS_OWNER);
    User savedOwner = userRepository.save(owner);

    CreateBusinessRequest firstRequest = new CreateBusinessRequest();
    firstRequest.setOwnerId(savedOwner.getId());
    firstRequest.setName("Clinic One");
    firstRequest.setDescription("First clinic");
    firstRequest.setAddress("Hyderabad");
    firstRequest.setTimezone("Asia/Kolkata");

    CreateBusinessRequest secondRequest = new CreateBusinessRequest();
    secondRequest.setOwnerId(savedOwner.getId());
    secondRequest.setName("Clinic Two");
    secondRequest.setDescription("Second clinic");
    secondRequest.setAddress("Secunderabad");
    secondRequest.setTimezone("Asia/Kolkata");

    mockMvc.perform(
            post("/api/v1/businesses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest))
    ).andExpect(status().isCreated());

    mockMvc.perform(
            post("/api/v1/businesses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondRequest))
    ).andExpect(status().isCreated());

    mockMvc.perform(
            get("/api/v1/businesses")
                    .param("ownerId", savedOwner.getId().toString())
                    .param("page", "0")
                    .param("size", "10")
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].name").value("Clinic One"))
            .andExpect(jsonPath("$.content[1].name").value("Clinic Two"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));
}

@Test
void shouldPaginateBusinessesByOwner() throws Exception {

    User owner = new User();
    owner.setName("Pagination Owner");
    owner.setEmail("pagination-owner@test.com");
    owner.setPhone("9999999995");
    owner.setPasswordHash("test-password");
    owner.setRole(UserRole.BUSINESS_OWNER);
    User savedOwner = userRepository.save(owner);

    for (int i = 1; i <= 3; i++) {
        CreateBusinessRequest request = new CreateBusinessRequest();
        request.setOwnerId(savedOwner.getId());
        request.setName("Clinic " + i);
        request.setDescription("Clinic " + i + " description");
        request.setAddress("Hyderabad");
        request.setTimezone("Asia/Kolkata");

        mockMvc.perform(
                post("/api/v1/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated());
    }

    mockMvc.perform(
            get("/api/v1/businesses")
                    .param("ownerId", savedOwner.getId().toString())
                    .param("page", "0")
                    .param("size", "2")
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].name").value("Clinic 1"))
            .andExpect(jsonPath("$.content[1].name").value("Clinic 2"))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false));

    mockMvc.perform(
            get("/api/v1/businesses")
                    .param("ownerId", savedOwner.getId().toString())
                    .param("page", "1")
                    .param("size", "2")
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Clinic 3"))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true));
}

@Test
void shouldReturnEmptyPageWhenOwnerHasNoBusinesses() throws Exception {

    User owner = new User();
    owner.setName("Empty Business Owner");
    owner.setEmail("empty-business-owner@test.com");
    owner.setPhone("9999999997");
    owner.setPasswordHash("test-password");
    owner.setRole(UserRole.BUSINESS_OWNER);
    User savedOwner = userRepository.save(owner);

    mockMvc.perform(
            get("/api/v1/businesses")
                    .param("ownerId", savedOwner.getId().toString())
                    .param("page", "0")
                    .param("size", "10")
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));
}

@Test
void shouldReturn404WhenListingBusinessesForMissingOwner() throws Exception {
    mockMvc.perform(
            get("/api/v1/businesses")
                    .param("ownerId", "999999")
    )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error")
                    .value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message")
                    .value("Business owner with id 999999 was not found"));
}

@Test
void shouldDeactivateBusinessSuccessfully() throws Exception {
    User owner = new User();
    owner.setName("Deactivate Owner");
    owner.setEmail("deactivate-owner@test.com");
    owner.setPhone("9999999996");
    owner.setPasswordHash("test-password");
    owner.setRole(UserRole.BUSINESS_OWNER);

    User savedOwner = userRepository.save(owner);

    CreateBusinessRequest request = new CreateBusinessRequest();
    request.setOwnerId(savedOwner.getId());
    request.setName("Business To Deactivate");
    request.setDescription("Business description");
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
            patch("/api/v1/businesses/{businessId}/deactivate", businessId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(businessId))
            .andExpect(jsonPath("$.name").value("Business To Deactivate"))
            .andExpect(jsonPath("$.status").value("INACTIVE"));
}

@Test
void shouldReturn409WhenBusinessIsAlreadyInactive() throws Exception {
    User owner = new User();
    owner.setName("Already Inactive Owner");
    owner.setEmail("already-inactive-owner@test.com");
    owner.setPhone("9999999995");
    owner.setPasswordHash("test-password");
    owner.setRole(UserRole.BUSINESS_OWNER);

    User savedOwner = userRepository.save(owner);

    CreateBusinessRequest request = new CreateBusinessRequest();
    request.setOwnerId(savedOwner.getId());
    request.setName("Already Inactive Business");
    request.setDescription("Business description");
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
            patch("/api/v1/businesses/{businessId}/deactivate", businessId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));

    mockMvc.perform(
            patch("/api/v1/businesses/{businessId}/deactivate", businessId)
    )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("CONFLICT"))
            .andExpect(jsonPath("$.message")
                    .value("Business with id " + businessId + " is already inactive"));
}

@Test
void shouldReturn404WhenDeactivatingMissingBusiness() throws Exception {
    mockMvc.perform(
            patch("/api/v1/businesses/{businessId}/deactivate", 999999L)
    )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error")
                    .value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message")
                    .value("Business with id 999999 was not found"));
}

@Test
void shouldAssignStaffSuccessfully() throws Exception {
    User owner = new User();
    owner.setName("Staff Business Owner");
    owner.setEmail("staff-business-owner@test.com");
    owner.setPhone("9999999991");
    owner.setPasswordHash("test-password");
    owner.setRole(UserRole.BUSINESS_OWNER);

    User savedOwner = userRepository.save(owner);

    CreateBusinessRequest businessRequest = new CreateBusinessRequest();
    businessRequest.setOwnerId(savedOwner.getId());
    businessRequest.setName("Staff Test Business");
    businessRequest.setDescription("Business for staff testing");
    businessRequest.setAddress("Hyderabad");
    businessRequest.setTimezone("Asia/Kolkata");

    String businessResponse = mockMvc.perform(
            post("/api/v1/businesses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(businessRequest))
    )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long businessId = objectMapper.readTree(businessResponse)
            .get("id")
            .asLong();

    User staff = new User();
    staff.setName("Test Staff");
    staff.setEmail("test-staff@test.com");
    staff.setPhone("9999999992");
    staff.setPasswordHash("test-password");
    staff.setRole(UserRole.STAFF);

    User savedStaff = userRepository.save(staff);

    AssignStaffRequest request = new AssignStaffRequest();
    request.setStaffId(savedStaff.getId());

    mockMvc.perform(
            post("/api/v1/businesses/{businessId}/staff", businessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.staffId").value(savedStaff.getId()))
            .andExpect(jsonPath("$.name").value("Test Staff"))
            .andExpect(jsonPath("$.email").value("test-staff@test.com"));
}

@Test
void shouldReturn409WhenStaffIsAlreadyAssigned() throws Exception {
    User owner = createUser(
            "Duplicate Staff Owner",
            "duplicate-staff-owner@test.com",
            UserRole.BUSINESS_OWNER
    );

    Long businessId = createBusiness(owner.getId(), "Duplicate Staff Business");

    User staff = createUser(
            "Duplicate Staff",
            "duplicate-staff@test.com",
            UserRole.STAFF
    );

    AssignStaffRequest request = new AssignStaffRequest();
    request.setStaffId(staff.getId());

    mockMvc.perform(
            post("/api/v1/businesses/{businessId}/staff", businessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isCreated());

    mockMvc.perform(
            post("/api/v1/businesses/{businessId}/staff", businessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("CONFLICT"));
}

@Test
void shouldReturn404WhenAssigningStaffToMissingBusiness() throws Exception {
    User staff = createUser(
            "Missing Business Staff",
            "missing-business-staff@test.com",
            UserRole.STAFF
    );

    AssignStaffRequest request = new AssignStaffRequest();
    request.setStaffId(staff.getId());

    mockMvc.perform(
            post("/api/v1/businesses/{businessId}/staff", 999999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error")
                    .value("RESOURCE_NOT_FOUND"));
}

@Test
void shouldReturn404WhenAssigningMissingStaff() throws Exception {
    User owner = createUser(
            "Missing Staff Owner",
            "missing-staff-owner@test.com",
            UserRole.BUSINESS_OWNER
    );

    Long businessId = createBusiness(
            owner.getId(),
            "Missing Staff Business"
    );

    AssignStaffRequest request = new AssignStaffRequest();
    request.setStaffId(999999L);

    mockMvc.perform(
            post("/api/v1/businesses/{businessId}/staff", businessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error")
                    .value("RESOURCE_NOT_FOUND"));
}

@Test
void shouldReturn400WhenAssigningNonStaffUser() throws Exception {
    User owner = createUser(
            "Non Staff Owner",
            "non-staff-owner@test.com",
            UserRole.BUSINESS_OWNER
    );

    Long businessId = createBusiness(
            owner.getId(),
            "Non Staff Business"
    );

    User customer = createUser(
            "Regular Customer",
            "regular-customer@test.com",
            UserRole.CUSTOMER
    );

    AssignStaffRequest request = new AssignStaffRequest();
    request.setStaffId(customer.getId());

    mockMvc.perform(
            post("/api/v1/businesses/{businessId}/staff", businessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error")
                    .value("BAD_REQUEST"));
}

@Test
void shouldListBusinessStaffSuccessfully() throws Exception {
    User owner = createUser(
            "List Staff Owner",
            "list-staff-owner@test.com",
            UserRole.BUSINESS_OWNER
    );

    Long businessId = createBusiness(
            owner.getId(),
            "List Staff Business"
    );

    User firstStaff = createUser(
            "First Staff",
            "first-staff@test.com",
            UserRole.STAFF
    );

    User secondStaff = createUser(
            "Second Staff",
            "second-staff@test.com",
            UserRole.STAFF
    );

    assignStaff(businessId, firstStaff.getId());
    assignStaff(businessId, secondStaff.getId());

    mockMvc.perform(
            get("/api/v1/businesses/{businessId}/staff", businessId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].name").value("First Staff"))
            .andExpect(jsonPath("$.content[1].name").value("Second Staff"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));
}

@Test
void shouldReturnEmptyListWhenBusinessHasNoStaff() throws Exception {
    User owner = createUser(
            "Empty Staff Owner",
            "empty-staff-owner@test.com",
            UserRole.BUSINESS_OWNER
    );

    Long businessId = createBusiness(
            owner.getId(),
            "Empty Staff Business"
    );

    mockMvc.perform(
            get("/api/v1/businesses/{businessId}/staff", businessId)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));
}

@Test
void shouldUseDefaultPaginationWhenPageAndSizeAreNotProvided() throws Exception {

    User owner = new User();
    owner.setName("Default Pagination Owner");
    owner.setEmail("default-pagination-owner@test.com");
    owner.setPhone("9999999996");
    owner.setPasswordHash("test-password");
    owner.setRole(UserRole.BUSINESS_OWNER);
    User savedOwner = userRepository.save(owner);

    CreateBusinessRequest request = new CreateBusinessRequest();
    request.setOwnerId(savedOwner.getId());
    request.setName("Default Pagination Business");
    request.setDescription("Default pagination test");
    request.setAddress("Hyderabad");
    request.setTimezone("Asia/Kolkata");

    mockMvc.perform(
            post("/api/v1/businesses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isCreated());

    mockMvc.perform(
            get("/api/v1/businesses")
                    .param("ownerId", savedOwner.getId().toString())
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.first").value(true));
}

@Test
void shouldCapPaginationSizeAtMaximum() throws Exception {

    User owner = new User();
    owner.setName("Maximum Pagination Owner");
    owner.setEmail("maximum-pagination-owner@test.com");
    owner.setPhone("9999999997");
    owner.setPasswordHash("test-password");
    owner.setRole(UserRole.BUSINESS_OWNER);
    User savedOwner = userRepository.save(owner);

    CreateBusinessRequest request = new CreateBusinessRequest();
    request.setOwnerId(savedOwner.getId());
    request.setName("Maximum Pagination Business");
    request.setDescription("Maximum pagination test");
    request.setAddress("Hyderabad");
    request.setTimezone("Asia/Kolkata");

    mockMvc.perform(
            post("/api/v1/businesses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isCreated());

    mockMvc.perform(
            get("/api/v1/businesses")
                    .param("ownerId", savedOwner.getId().toString())
                    .param("page", "0")
                    .param("size", "101")
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(100))
            .andExpect(jsonPath("$.first").value(true));
}

@Test
void shouldPaginateBusinessStaff() throws Exception {

    User owner = createUser(
            "Pagination Staff Owner",
            "pagination-staff-owner@test.com",
            UserRole.BUSINESS_OWNER
    );

    Long businessId = createBusiness(
            owner.getId(),
            "Pagination Staff Business"
    );

    User firstStaff = createUser(
            "Pagination Staff One",
            "pagination-staff-one@test.com",
            UserRole.STAFF
    );

    User secondStaff = createUser(
            "Pagination Staff Two",
            "pagination-staff-two@test.com",
            UserRole.STAFF
    );

    User thirdStaff = createUser(
            "Pagination Staff Three",
            "pagination-staff-three@test.com",
            UserRole.STAFF
    );

    assignStaff(businessId, firstStaff.getId());
    assignStaff(businessId, secondStaff.getId());
    assignStaff(businessId, thirdStaff.getId());

    mockMvc.perform(
            get("/api/v1/businesses/{businessId}/staff", businessId)
                    .param("page", "0")
                    .param("size", "2")
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false));

    mockMvc.perform(
            get("/api/v1/businesses/{businessId}/staff", businessId)
                    .param("page", "1")
                    .param("size", "2")
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true));
}

@Test
void shouldReturn404WhenListingStaffForMissingBusiness() throws Exception {
    mockMvc.perform(
            get("/api/v1/businesses/{businessId}/staff", 999999L)
    )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error")
                    .value("RESOURCE_NOT_FOUND"));
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

private Long createBusiness(
        Long ownerId,
        String name
) throws Exception {
    CreateBusinessRequest request = new CreateBusinessRequest();
    request.setOwnerId(ownerId);
    request.setName(name);
    request.setDescription("Test business");
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

    return objectMapper.readTree(response)
            .get("id")
            .asLong();
}

private void assignStaff(
        Long businessId,
        Long staffId
) throws Exception {
    AssignStaffRequest request = new AssignStaffRequest();
    request.setStaffId(staffId);

    mockMvc.perform(
            post("/api/v1/businesses/{businessId}/staff", businessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
    )
            .andExpect(status().isCreated());
}

}
