package com.queueflow.business.dto;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessStatus;

public class BusinessResponse {

    private Long id;
    private String name;
    private String description;
    private String address;
    private String timezone;
    private BusinessStatus status;

    public static BusinessResponse from(Business business) {
        BusinessResponse response = new BusinessResponse();

        response.id = business.getId();
        response.name = business.getName();
        response.description = business.getDescription();
        response.address = business.getAddress();
        response.timezone = business.getTimezone();
        response.status = business.getStatus();

        return response;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getTimezone() {
        return timezone;
    }

    public BusinessStatus getStatus() {
        return status;
    }
}