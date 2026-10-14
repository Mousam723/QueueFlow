package com.queueflow.customer.controller;

import com.queueflow.business.dto.BusinessResponse;
import com.queueflow.customer.service.CustomerDiscoveryService;
import com.queueflow.queue.dto.QueueResponse;
import com.queueflow.service.dto.ServiceResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/customer")
public class CustomerDiscoveryController {

    private final CustomerDiscoveryService customerDiscoveryService;

    public CustomerDiscoveryController(
            CustomerDiscoveryService customerDiscoveryService
    ) {
        this.customerDiscoveryService = customerDiscoveryService;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/businesses")
    public Page<BusinessResponse> getActiveBusinesses(
            Pageable pageable
    ) {
        return customerDiscoveryService.getActiveBusinesses(pageable);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/businesses/{businessId}/services")
    public Page<ServiceResponse> getActiveServices(
            @PathVariable Long businessId,
            Pageable pageable
    ) {
        return customerDiscoveryService.getActiveServices(
                businessId,
                pageable
        );
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping(
            "/businesses/{businessId}/services/{serviceId}/queues"
    )
    public QueueResponse getActiveQueue(
            @PathVariable Long businessId,
            @PathVariable Long serviceId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return customerDiscoveryService.getActiveQueue(
                businessId,
                serviceId,
                date
        );
    }
}
