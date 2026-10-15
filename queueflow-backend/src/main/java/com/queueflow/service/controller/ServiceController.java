package com.queueflow.service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.queueflow.service.dto.CreateServiceRequest;
import com.queueflow.service.dto.ServiceResponse;
import com.queueflow.service.service.ServiceService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;


@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/businesses/{businessId}/services")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @PreAuthorize("@authorizationService.isAdmin() || @authorizationService.isBusinessOwner(#businessId)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse createService(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateServiceRequest request) {

        return serviceService.createService(businessId, request);
    }

    @PreAuthorize("@authorizationService.canAccessBusiness(#businessId)")
    @GetMapping
    public Page<ServiceResponse> getServices(
            @PathVariable Long businessId,
            Pageable pageable) {

        return serviceService.getServicesByBusiness(businessId, pageable);
    }

    @PreAuthorize("@authorizationService.isAdmin() || @authorizationService.isBusinessOwner(#businessId)")
    @PatchMapping("/{serviceId}/deactivate")
    public ServiceResponse deactivateService(
            @PathVariable Long businessId,
            @PathVariable Long serviceId) {
        return serviceService.deactivateService(businessId, serviceId);
    }

    @PreAuthorize("@authorizationService.isAdmin() || @authorizationService.isBusinessOwner(#businessId)")
    @PatchMapping("/{serviceId}/reactivate")
    public ServiceResponse reactivateService(
            @PathVariable Long businessId,
            @PathVariable Long serviceId) {
        return serviceService.reactivateService(businessId, serviceId);
    }

}
