package com.queueflow.appointment.dto;

import com.queueflow.appointment.Appointment;
import com.queueflow.appointment.AppointmentStatus;

import java.time.LocalDate;

public class AppointmentResponse {

    private Long id;
    private Long customerId;
    private Long businessId;
    private Long serviceId;
    private Long queueId;
    private Integer tokenNumber;
    private LocalDate appointmentDate;
    private AppointmentStatus status;

    public static AppointmentResponse from(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();

        response.id = appointment.getId();
        response.customerId = appointment.getCustomer().getId();
        response.businessId = appointment.getBusiness().getId();
        response.serviceId = appointment.getService().getId();
        response.queueId = appointment.getQueue().getId();
        response.tokenNumber = appointment.getTokenNumber();
        response.appointmentDate = appointment.getAppointmentDate();
        response.status = appointment.getStatus();

        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public Long getQueueId() {
        return queueId;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public AppointmentStatus getStatus() {
        return status;
    }
}