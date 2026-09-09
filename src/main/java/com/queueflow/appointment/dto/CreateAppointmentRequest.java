package com.queueflow.appointment.dto;

import jakarta.validation.constraints.NotNull;

public class CreateAppointmentRequest {

    @NotNull
    private Long customerId;

    @NotNull
    private Long queueId;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(Long queueId) {
        this.queueId = queueId;
    }
}