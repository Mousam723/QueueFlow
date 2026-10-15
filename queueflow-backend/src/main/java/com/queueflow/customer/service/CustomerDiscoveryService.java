package com.queueflow.customer.service;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.business.BusinessStatus;
import com.queueflow.business.dto.BusinessResponse;
import com.queueflow.common.exception.BadRequestException;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.queue.QueueRepository;
import com.queueflow.queue.QueueStatus;
import com.queueflow.queue.dto.QueueResponse;
import com.queueflow.service.ServiceRepository;
import com.queueflow.service.ServiceStatus;
import com.queueflow.service.dto.ServiceResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@org.springframework.stereotype.Service
public class CustomerDiscoveryService {

    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;
    private final QueueRepository queueRepository;

    public CustomerDiscoveryService(
            BusinessRepository businessRepository,
            ServiceRepository serviceRepository,
            QueueRepository queueRepository
    ) {
        this.businessRepository = businessRepository;
        this.serviceRepository = serviceRepository;
        this.queueRepository = queueRepository;
    }

    @Transactional(readOnly = true)
    public Page<BusinessResponse> getActiveBusinesses(
            Pageable pageable
    ) {
        return businessRepository
                .findByStatusOrderByIdAsc(
                        BusinessStatus.ACTIVE,
                        pageable
                )
                .map(BusinessResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getActiveServices(
            Long businessId,
            Pageable pageable
    ) {
        Business business = getActiveBusiness(businessId);

        return serviceRepository
                .findByBusinessIdAndStatusOrderByIdAsc(
                        business.getId(),
                        ServiceStatus.ACTIVE,
                        pageable
                )
                .map(ServiceResponse::from);
    }

    @Transactional(readOnly = true)
    public QueueResponse getActiveQueue(
            Long businessId,
            Long serviceId,
            LocalDate queueDate
    ) {
        Business business = getActiveBusiness(businessId);

        com.queueflow.service.Service service =
                serviceRepository.findById(serviceId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Service with id " + serviceId
                                                + " was not found"
                                )
                        );

        if (!service.getBusiness().getId().equals(business.getId())) {
            throw new BadRequestException(
                    "Service does not belong to business with id "
                            + businessId
            );
        }

        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new BadRequestException(
                    "Service with id " + serviceId
                            + " is not active"
            );
        }

        LocalDate date = queueDate != null
                ? queueDate
                : LocalDate.now();

        return queueRepository
                .findByBusinessIdAndServiceIdAndQueueDateAndStatus(
                        businessId,
                        serviceId,
                        date,
                        QueueStatus.ACTIVE
                )
                .map(QueueResponse::from)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No active queue was found for service "
                                        + serviceId
                                        + " on "
                                        + date
                        )
                );
    }

    private Business getActiveBusiness(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Business with id " + businessId
                                        + " was not found"
                        )
                );

        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new ResourceNotFoundException(
                    "Business with id " + businessId
                            + " was not found"
            );
        }

        return business;
    }
}
