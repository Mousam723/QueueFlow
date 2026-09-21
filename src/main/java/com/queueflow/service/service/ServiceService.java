package com.queueflow.service.service;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.service.ServiceRepository;
import com.queueflow.service.ServiceStatus;
import com.queueflow.service.dto.CreateServiceRequest;
import com.queueflow.service.dto.ServiceResponse;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;

    public ServiceService(
            ServiceRepository serviceRepository,
            BusinessRepository businessRepository) {

        this.serviceRepository = serviceRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional
    public ServiceResponse createService(
            Long businessId,
            CreateServiceRequest request) {

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Business with id " + businessId + " was not found"));

        com.queueflow.service.Service service =
                new com.queueflow.service.Service();

        service.setBusiness(business);
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setStatus(ServiceStatus.ACTIVE);

        com.queueflow.service.Service savedService =
                serviceRepository.save(service);

        return ServiceResponse.from(savedService);
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getServicesByBusiness(
            Long businessId,
            Pageable pageable
    ) {

        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException(
                    "Business with id " + businessId + " was not found");
        }

        return serviceRepository
                .findByBusinessIdOrderByIdAsc(businessId, pageable)
                .map(ServiceResponse::from);
    }
}
