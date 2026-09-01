package com.queueflow.business.controller;

import com.queueflow.business.dto.BusinessResponse;
import com.queueflow.business.dto.CreateBusinessRequest;
import com.queueflow.business.service.BusinessService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
            @Valid @RequestBody CreateBusinessRequest request) {

        return businessService.createBusiness(request);
    }
}