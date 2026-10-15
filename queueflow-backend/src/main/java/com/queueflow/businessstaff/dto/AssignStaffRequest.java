package com.queueflow.businessstaff.dto;

import jakarta.validation.constraints.NotNull;

public class AssignStaffRequest {

    @NotNull
    private Long staffId;

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }
}
