package com.queueflow.businessstaff.dto;

import com.queueflow.businessstaff.BusinessStaff;
import com.queueflow.user.User;

public class StaffResponse {

    private Long id;
    private Long staffId;
    private String name;
    private String email;
    private String phone;

    public static StaffResponse from(BusinessStaff businessStaff) {
        User staff = businessStaff.getStaff();

        StaffResponse response = new StaffResponse();
        response.id = businessStaff.getId();
        response.staffId = staff.getId();
        response.name = staff.getName();
        response.email = staff.getEmail();
        response.phone = staff.getPhone();

        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getStaffId() {
        return staffId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
