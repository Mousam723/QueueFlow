package com.queueflow.business.service;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.business.BusinessStatus;
import com.queueflow.business.dto.BusinessResponse;
import com.queueflow.business.dto.CreateBusinessRequest;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    public BusinessService(
            BusinessRepository businessRepository,
            UserRepository userRepository) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BusinessResponse createBusiness(CreateBusinessRequest request) {

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                "Business owner with id " + request.getOwnerId() + " was not found"));

        Business business = new Business();

        business.setOwner(owner);
        business.setName(request.getName());
        business.setDescription(request.getDescription());
        business.setAddress(request.getAddress());
        business.setTimezone(request.getTimezone());
        business.setStatus(BusinessStatus.ACTIVE);

        Business savedBusiness = businessRepository.save(business);

        return BusinessResponse.from(savedBusiness);
    }

    @Transactional(readOnly = true)
public BusinessResponse getBusiness(Long businessId) {
    Business business = businessRepository.findById(businessId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Business with id " + businessId + " was not found"
                    ));

    return BusinessResponse.from(business);
}

@Transactional(readOnly = true)
public Page<BusinessResponse> getBusinessesByOwner(Long ownerId,Pageable pageable) {
    if (!userRepository.existsById(ownerId)) {
        throw new ResourceNotFoundException(
                "Business owner with id " + ownerId + " was not found"
        );
    }

    return businessRepository.findByOwnerIdOrderByIdAsc(ownerId, pageable)
        .map(BusinessResponse::from);
}

@Transactional
public BusinessResponse deactivateBusiness(Long businessId) {
    Business business = businessRepository.findById(businessId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Business with id " + businessId + " was not found"
                    ));

    if (business.getStatus() == BusinessStatus.INACTIVE) {
        throw new ConflictException(
                "Business with id " + businessId + " is already inactive"
        );
    }

    business.setStatus(BusinessStatus.INACTIVE);

    Business savedBusiness = businessRepository.save(business);

    return BusinessResponse.from(savedBusiness);
}

}
