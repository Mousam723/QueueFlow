package com.queueflow.service.controller;

import com.queueflow.service.dto.CreateServiceRequest;
import com.queueflow.service.dto.ServiceResponse;
import com.queueflow.service.service.ServiceService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/services")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse createService(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateServiceRequest request) {

        return serviceService.createService(businessId, request);
    }

    @GetMapping
    public List<ServiceResponse> getServices(
            @PathVariable Long businessId) {

        return serviceService.getServicesByBusiness(businessId);
    }
}
