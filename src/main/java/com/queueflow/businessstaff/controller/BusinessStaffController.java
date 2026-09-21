package com.queueflow.businessstaff.controller;

import com.queueflow.businessstaff.dto.AssignStaffRequest;
import com.queueflow.businessstaff.dto.StaffResponse;
import com.queueflow.businessstaff.service.BusinessStaffService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/businesses/{businessId}/staff")
public class BusinessStaffController {

    private final BusinessStaffService businessStaffService;

    public BusinessStaffController(
            BusinessStaffService businessStaffService
    ) {
        this.businessStaffService = businessStaffService;
    }

    @PreAuthorize("@authorizationService.isAdmin() || @authorizationService.isBusinessOwner(#businessId)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse assignStaff(
            @PathVariable Long businessId,
            @Valid @RequestBody AssignStaffRequest request
    ) {
        return businessStaffService.assignStaff(businessId, request);
    }

    @PreAuthorize("@authorizationService.isAdmin() || @authorizationService.isBusinessOwner(#businessId)")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<StaffResponse> getBusinessStaff(
            @PathVariable Long businessId,
            Pageable pageable
    ) {
        return businessStaffService.getBusinessStaff(
                businessId,
                pageable
        );
    }
}
