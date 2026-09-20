package com.queueflow.business.controller;

import com.queueflow.business.dto.BusinessResponse;
import com.queueflow.business.dto.CreateBusinessRequest;
import com.queueflow.business.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
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

    @PreAuthorize("@authorizationService.isAdmin() || @authorizationService.isBusinessOwner(#businessId)")
    @PatchMapping("/{businessId}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public BusinessResponse deactivateBusiness(
            @PathVariable Long businessId
    ) {
        return businessService.deactivateBusiness(businessId);
    }
}