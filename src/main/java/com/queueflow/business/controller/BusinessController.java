package com.queueflow.business.controller;

import com.queueflow.business.dto.BusinessResponse;
import com.queueflow.business.dto.CreateBusinessRequest;
import com.queueflow.business.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

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

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<BusinessResponse> getBusinessesByOwner(
            @RequestParam Long ownerId
    ) {
        return businessService.getBusinessesByOwner(ownerId);
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