package com.queueflow.queue.service;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.common.exception.BadRequestException;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueRepository;
import com.queueflow.queue.QueueStatus;
import com.queueflow.queue.dto.CreateQueueRequest;
import com.queueflow.queue.dto.QueueResponse;
import com.queueflow.service.Service;
import com.queueflow.service.ServiceRepository;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@org.springframework.stereotype.Service
public class QueueService {

    private final QueueRepository queueRepository;
    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;

    public QueueService(
            QueueRepository queueRepository,
            BusinessRepository businessRepository,
            ServiceRepository serviceRepository) {

        this.queueRepository = queueRepository;
        this.businessRepository = businessRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional
    public QueueResponse createQueue(
            Long businessId,
            CreateQueueRequest request) {

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Business with id " + businessId + " was not found"
                        ));

        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Service with id " + request.getServiceId()
                                        + " was not found"
                        ));

        if (!service.getBusiness().getId().equals(businessId)) {
            throw new BadRequestException(
                    "Service does not belong to business with id " + businessId
            );
        }

        LocalDate queueDate = request.getQueueDate() != null
                ? request.getQueueDate()
                : LocalDate.now();

        if (queueRepository
                .findByServiceIdAndQueueDate(
                        service.getId(),
                        queueDate
                )
                .isPresent()) {

            throw new ConflictException(
                    "Queue already exists for service "
                            + service.getId()
                            + " on "
                            + queueDate
            );
        }

        Queue queue = new Queue();

        queue.setBusiness(business);
        queue.setService(service);
        queue.setQueueDate(queueDate);
        queue.setStatus(QueueStatus.ACTIVE);
        queue.setCurrentToken(0);

        Queue savedQueue = queueRepository.save(queue);

        return QueueResponse.from(savedQueue);
    }

    @Transactional(readOnly = true)
    public QueueResponse getQueue(Long queueId) {

        Queue queue = queueRepository.findById(queueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Queue with id " + queueId + " was not found"
                        ));

        return QueueResponse.from(queue);
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> getBusinessQueues(
            Long businessId,
            LocalDate queueDate) {

        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException(
                    "Business with id " + businessId + " was not found"
            );
        }

        LocalDate date = queueDate != null
                ? queueDate
                : LocalDate.now();

        return queueRepository
                .findByBusinessIdAndQueueDate(businessId, date)
                .stream()
                .map(QueueResponse::from)
                .toList();
    }
}
