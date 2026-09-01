package com.queueflow.queue.controller;

import com.queueflow.queue.dto.QueueResponse;
import com.queueflow.queue.service.QueueService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queues")
public class QueueDetailsController {

    private final QueueService queueService;

    public QueueDetailsController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/{queueId}")
    public QueueResponse getQueue(
            @PathVariable Long queueId) {

        return queueService.getQueue(queueId);
    }
}