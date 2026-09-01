package com.queueflow.service.dto;

import com.queueflow.service.Service;
import com.queueflow.service.ServiceStatus;

public class ServiceResponse {

    private Long id;
    private Long businessId;
    private String name;
    private String description;
    private Integer durationMinutes;
    private ServiceStatus status;

    public static ServiceResponse from(Service service) {
        ServiceResponse response = new ServiceResponse();

        response.id = service.getId();
        response.businessId = service.getBusiness().getId();
        response.name = service.getName();
        response.description = service.getDescription();
        response.durationMinutes = service.getDurationMinutes();
        response.status = service.getStatus();

        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public ServiceStatus getStatus() {
        return status;
    }
}
