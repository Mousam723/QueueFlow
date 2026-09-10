package com.queueflow.appointment.dto;

import com.queueflow.appointment.AppointmentStatus;

public class QueuePositionResponse {

    private Long appointmentId;
    private Integer tokenNumber;
    private AppointmentStatus status;
    private Integer queuePosition;
    private Integer estimatedWaitMinutes;

    public QueuePositionResponse(
            Long appointmentId,
            Integer tokenNumber,
            AppointmentStatus status,
            Integer queuePosition,
            Integer estimatedWaitMinutes
    ) {
        this.appointmentId = appointmentId;
        this.tokenNumber = tokenNumber;
        this.status = status;
        this.queuePosition = queuePosition;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public Integer getQueuePosition() {
        return queuePosition;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }
}