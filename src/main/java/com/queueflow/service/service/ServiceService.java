package com.queueflow.service.service;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.common.exception.BadRequestException;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.service.ServiceRepository;
import com.queueflow.service.ServiceStatus;
import com.queueflow.service.dto.CreateServiceRequest;
import com.queueflow.service.dto.ServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ServiceResponse deactivateService(
            Long businessId,
            Long serviceId
    ) {
        com.queueflow.service.Service service =
                getServiceForBusiness(businessId, serviceId);

        if (service.getStatus() == ServiceStatus.INACTIVE) {
            throw new ConflictException(
                    "Service with id " + serviceId + " is already inactive");
        }

        service.setStatus(ServiceStatus.INACTIVE);

        com.queueflow.service.Service savedService =
                serviceRepository.save(service);

        return ServiceResponse.from(savedService);
    }

    @Transactional
    public ServiceResponse reactivateService(
            Long businessId,
            Long serviceId
    ) {
        com.queueflow.service.Service service =
                getServiceForBusiness(businessId, serviceId);

        if (service.getStatus() == ServiceStatus.ACTIVE) {
            throw new ConflictException(
                    "Service with id " + serviceId + " is already active");
        }

        service.setStatus(ServiceStatus.ACTIVE);

        com.queueflow.service.Service savedService =
                serviceRepository.save(service);

        return ServiceResponse.from(savedService);
    }

    private com.queueflow.service.Service getServiceForBusiness(
            Long businessId,
            Long serviceId
    ) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException(
                    "Business with id " + businessId + " was not found");
        }

        com.queueflow.service.Service service =
                serviceRepository.findById(serviceId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Service with id " + serviceId
                                                + " was not found"));

        if (!service.getBusiness().getId().equals(businessId)) {
            throw new BadRequestException(
                    "Service does not belong to business with id "
                            + businessId);
        }

        return service;
    }
}
