package com.queueflow.queue.controller;

import com.queueflow.queue.dto.CreateQueueRequest;
import com.queueflow.queue.dto.QueueResponse;
import com.queueflow.queue.service.QueueService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/queues")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QueueResponse createQueue(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateQueueRequest request) {

        return queueService.createQueue(
                businessId,
                request
        );
    }

    @GetMapping
    public List<QueueResponse> getBusinessQueues(
            @PathVariable Long businessId,
            @RequestParam(required = false) LocalDate date) {

        return queueService.getBusinessQueues(
                businessId,
                date
        );
    }
}