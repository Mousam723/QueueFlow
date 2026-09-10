package com.queueflow.businessstaff.controller;

import com.queueflow.businessstaff.dto.AssignStaffRequest;
import com.queueflow.businessstaff.dto.StaffResponse;
import com.queueflow.businessstaff.service.BusinessStaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/staff")
public class BusinessStaffController {

    private final BusinessStaffService businessStaffService;

    public BusinessStaffController(
            BusinessStaffService businessStaffService
    ) {
        this.businessStaffService = businessStaffService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse assignStaff(
            @PathVariable Long businessId,
            @Valid @RequestBody AssignStaffRequest request
    ) {
        return businessStaffService.assignStaff(businessId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<StaffResponse> getBusinessStaff(
            @PathVariable Long businessId
    ) {
        return businessStaffService.getBusinessStaff(businessId);
    }
}
