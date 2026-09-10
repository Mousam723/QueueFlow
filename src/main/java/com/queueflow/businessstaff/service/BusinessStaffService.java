package com.queueflow.businessstaff.service;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.businessstaff.BusinessStaff;
import com.queueflow.businessstaff.BusinessStaffRepository;
import com.queueflow.businessstaff.dto.AssignStaffRequest;
import com.queueflow.businessstaff.dto.StaffResponse;
import com.queueflow.common.exception.BadRequestException;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusinessStaffService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final BusinessStaffRepository businessStaffRepository;

    public BusinessStaffService(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            BusinessStaffRepository businessStaffRepository
    ) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.businessStaffRepository = businessStaffRepository;
    }

    @Transactional
    public StaffResponse assignStaff(
            Long businessId,
            AssignStaffRequest request
    ) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Business with id " + businessId + " was not found"
                        ));

        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Staff with id " + request.getStaffId() + " was not found"
                        ));

        if (staff.getRole() != UserRole.STAFF) {
            throw new BadRequestException(
                    "User with id " + request.getStaffId()
                            + " does not have STAFF role"
            );
        }

        if (businessStaffRepository.existsByBusinessIdAndStaffId(
                businessId,
                request.getStaffId()
        )) {
            throw new ConflictException(
                    "Staff with id " + request.getStaffId()
                            + " is already assigned to business with id "
                            + businessId
            );
        }

        BusinessStaff businessStaff = new BusinessStaff();
        businessStaff.setBusiness(business);
        businessStaff.setStaff(staff);

        BusinessStaff saved = businessStaffRepository.save(businessStaff);

        return StaffResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getBusinessStaff(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException(
                    "Business with id " + businessId + " was not found"
            );
        }

        return businessStaffRepository
                .findByBusinessIdOrderByCreatedAtAsc(businessId)
                .stream()
                .map(StaffResponse::from)
                .toList();
    }
}
