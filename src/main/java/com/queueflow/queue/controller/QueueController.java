package com.queueflow.queue.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.queueflow.queue.dto.CreateQueueRequest;
import com.queueflow.queue.dto.QueueResponse;
import com.queueflow.queue.service.QueueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PreAuthorize("@authorizationService.isAdmin() || @authorizationService.isBusinessOwner(#businessId)")
    @PostMapping("/businesses/{businessId}/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueResponse createQueue(
            @PathVariable Long businessId,
            @RequestBody CreateQueueRequest request) {

        return queueService.createQueue(businessId, request);
    }

    @PreAuthorize("@authorizationService.canAccessBusiness(#businessId)")
    @GetMapping("/businesses/{businessId}/queues")
    public Page<QueueResponse> getBusinessQueues(
            @PathVariable Long businessId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Pageable pageable) {

        return queueService.getBusinessQueues(
                businessId,
                date,
                pageable
        );
    }

    @PreAuthorize("@authorizationService.canAccessQueue(#queueId)")
    @GetMapping("/queues/{queueId}")
    public QueueResponse getQueue(
            @PathVariable Long queueId) {

        return queueService.getQueue(queueId);
    }

    @PostMapping("/queues/{queueId}/next")
    @ResponseStatus(HttpStatus.OK)
    public QueueResponse advanceQueue(
            @PathVariable Long queueId) {

        return queueService.advanceQueue(queueId);
    }

    @PostMapping("/queues/{queueId}/pause")
    @ResponseStatus(HttpStatus.OK)
    public QueueResponse pauseQueue(
            @PathVariable Long queueId) {

        return queueService.pauseQueue(queueId);
    }

    @PostMapping("/queues/{queueId}/resume")
    @ResponseStatus(HttpStatus.OK)
    public QueueResponse resumeQueue(
            @PathVariable Long queueId) {

        return queueService.resumeQueue(queueId);
    }

    @PostMapping("/queues/{queueId}/close")
    @ResponseStatus(HttpStatus.OK)
    public QueueResponse closeQueue(
            @PathVariable Long queueId) {

        return queueService.closeQueue(queueId);
    }
}