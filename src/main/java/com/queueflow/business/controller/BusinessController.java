package com.queueflow.business.controller;

import com.queueflow.auth.AuthorizationService;
import com.queueflow.business.dto.BusinessResponse;
import com.queueflow.business.dto.CreateBusinessRequest;
import com.queueflow.business.service.BusinessService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;
    private final AuthorizationService authorizationService;

    public BusinessController(
            BusinessService businessService,
            AuthorizationService authorizationService
    ) {
        this.businessService = businessService;
        this.authorizationService = authorizationService;
    }

    @PreAuthorize("@authorizationService.canCreateBusiness(#request.ownerId)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessResponse createBusiness(
            @Valid @RequestBody CreateBusinessRequest request
    ) {
        return businessService.createBusiness(request);
    }

    @PreAuthorize("@authorizationService.canAccessBusiness(#businessId)")
    @GetMapping("/{businessId}")
    @ResponseStatus(HttpStatus.OK)
    public BusinessResponse getBusiness(
            @PathVariable Long businessId
    ) {
        return businessService.getBusiness(businessId);
    }

    @PreAuthorize("@authorizationService.isAdmin() || @authorizationService.isBusinessOwnerId(#ownerId)")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<BusinessResponse> getBusinessesByOwner(
            @RequestParam Long ownerId,
            Pageable pageable
    ) {
        return businessService.getBusinessesByOwner(ownerId, pageable);
    }

    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'STAFF', 'ADMIN')")
    @GetMapping("/mine")
    @ResponseStatus(HttpStatus.OK)
    public Page<BusinessResponse> getMyBusinesses(
            Pageable pageable
    ) {
        Long userId = authorizationService.getAuthenticatedUserId();

        if (authorizationService.isAdmin()) {
            return businessService.getAllBusinesses(pageable);
        }

        if (authorizationService.isBusinessOwnerRole()) {
            return businessService.getBusinessesByOwner(userId, pageable);
        }

        return businessService.getBusinessesForStaff(userId, pageable);
    }

    @PreAuthorize("@authorizationService.isAdmin() || @authorizationService.isBusinessOwner(#businessId)")
    @PatchMapping("/{businessId}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public BusinessResponse deactivateBusiness(
            @PathVariable Long businessId
    ) {
        return businessService.deactivateBusiness(businessId);
    }
}
