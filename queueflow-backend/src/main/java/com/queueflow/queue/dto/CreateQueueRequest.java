package com.queueflow.queue.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateQueueRequest {

    @NotNull
    private Long serviceId;

    private LocalDate queueDate;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
    }
}