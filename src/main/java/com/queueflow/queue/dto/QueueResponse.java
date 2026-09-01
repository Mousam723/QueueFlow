package com.queueflow.queue.dto;

import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueStatus;

import java.time.LocalDate;

public class QueueResponse {

    private Long id;
    private Long businessId;
    private Long serviceId;
    private LocalDate queueDate;
    private QueueStatus status;
    private Integer currentToken;

    public static QueueResponse from(Queue queue) {

        QueueResponse response = new QueueResponse();

        response.id = queue.getId();
        response.businessId = queue.getBusiness().getId();
        response.serviceId = queue.getService().getId();
        response.queueDate = queue.getQueueDate();
        response.status = queue.getStatus();
        response.currentToken = queue.getCurrentToken();

        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public Integer getCurrentToken() {
        return currentToken;
    }
}