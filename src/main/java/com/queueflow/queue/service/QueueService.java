package com.queueflow.queue.service;

import com.queueflow.appointment.Appointment;
import com.queueflow.appointment.AppointmentRepository;
import com.queueflow.appointment.AppointmentStatus;
import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.common.exception.BadRequestException;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueRepository;
import com.queueflow.queue.QueueStatus;
import com.queueflow.queue.dto.CreateQueueRequest;
import com.queueflow.queue.dto.QueueResponse;
import com.queueflow.service.Service;
import com.queueflow.service.ServiceRepository;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@org.springframework.stereotype.Service
public class QueueService {
    private static final Logger log =
        LoggerFactory.getLogger(QueueService.class);

    private final QueueRepository queueRepository;
    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;

    public QueueService(
            QueueRepository queueRepository,
            BusinessRepository businessRepository,
            ServiceRepository serviceRepository,
            AppointmentRepository appointmentRepository) {

        this.queueRepository = queueRepository;
        this.businessRepository = businessRepository;
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
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
        queue.setNextTokenNumber(0);

        Queue savedQueue = queueRepository.save(queue);

        log.info(
            "queue.created queueId={} businessId={} serviceId={} queueDate={}",
            savedQueue.getId(),
            businessId,
            service.getId(),
            queueDate
        );

        return QueueResponse.from(savedQueue);
    }

    @Transactional(readOnly = true)
    public QueueResponse getQueue(Long queueId) {
        return QueueResponse.from(getQueueEntity(queueId));
    }

    /**
     * Advances the queue to the earliest WAITING appointment.
     *
     * Lock order:
     *
     *     Queue
     *       ↓
     *     Appointment
     *
     * The queue is locked first so concurrent queue operations
     * for the same queue are serialized.
     *
     * The selected appointment is also pessimistically locked
     * before changing WAITING -> CALLED.
     */
    @Transactional
    public QueueResponse advanceQueue(Long queueId) {

        /*
         * Step 1:
         * Lock the queue.
         */
        Queue queue = queueRepository.findByIdForUpdate(queueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Queue with id " + queueId + " was not found"
                        ));

        /*
         * Step 2:
         * Only ACTIVE queues can advance.
         */
        if (queue.getStatus() != QueueStatus.ACTIVE) {
            throw new ConflictException(
                    "Queue with id " + queueId + " is not active"
            );
        }

        /*
         * Step 3:
         * Find the earliest WAITING appointment while
         * pessimistically locking the appointment rows.
         *
         * This prevents another transaction from modifying
         * the selected appointment while this transaction is
         * deciding which appointment to call.
         */
        List<Appointment> waitingAppointments =
                appointmentRepository
                        .findByQueueIdAndStatusForUpdate(
                                queueId,
                                AppointmentStatus.WAITING
                        );

        /*
         * Step 4:
         * There is nobody waiting.
         */
        if (waitingAppointments.isEmpty()) {
            throw new ConflictException(
                    "No waiting appointments in queue with id " + queueId
            );
        }

        /*
         * Step 5:
         * The repository sorts by token number, so the first
         * appointment is the next appointment to be called.
         */
        Appointment nextAppointment = waitingAppointments.get(0);

        /*
         * Step 6:
         * Move appointment through its lifecycle.
         *
         * WAITING -> CALLED
         */
        nextAppointment.setStatus(AppointmentStatus.CALLED);

        appointmentRepository.save(nextAppointment);

        /*
         * Step 7:
         * Queue currentToken represents the token that was
         * most recently called.
         */
        queue.setCurrentToken(nextAppointment.getTokenNumber());

        Queue savedQueue = queueRepository.save(queue);

        log.info(
            "queue.advanced queueId={} tokenNumber={} appointmentId={}",
            queueId,
            nextAppointment.getTokenNumber(),
            nextAppointment.getId()
        );

        return QueueResponse.from(savedQueue);
    }

    @Transactional(readOnly = true)
    public Page<QueueResponse> getBusinessQueues(
            Long businessId,
            LocalDate queueDate,
            Pageable pageable) {

        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException(
                    "Business with id " + businessId + " was not found"
            );
        }

        LocalDate date = queueDate != null
                ? queueDate
                : LocalDate.now();

        return queueRepository
                .findByBusinessIdAndQueueDateOrderByIdAsc(
                        businessId,
                        date,
                        pageable
                )
                .map(QueueResponse::from);
    }

    @Transactional
    public QueueResponse pauseQueue(Long queueId) {

        Queue queue = getQueueEntityForUpdate(queueId);

        if (!queue.getStatus().canTransitionTo(QueueStatus.PAUSED)) {
            throw new ConflictException(
                    "Queue with id " + queueId
                            + " cannot transition from "
                            + queue.getStatus()
                            + " to PAUSED"
            );
        }

        queue.setStatus(QueueStatus.PAUSED);

        Queue savedQueue = queueRepository.save(queue);

        log.info(
            "queue.paused queueId={} previousStatus={} newStatus={}",
            queueId,
            QueueStatus.ACTIVE,
            savedQueue.getStatus()
        );

        return QueueResponse.from(savedQueue);
    }

    @Transactional
    public QueueResponse resumeQueue(Long queueId) {

        Queue queue = getQueueEntityForUpdate(queueId);

        if (!queue.getStatus().canTransitionTo(QueueStatus.ACTIVE)) {
            throw new ConflictException(
                    "Queue with id " + queueId
                            + " cannot transition from "
                            + queue.getStatus()
                            + " to ACTIVE"
            );
        }

        queue.setStatus(QueueStatus.ACTIVE);

        Queue savedQueue = queueRepository.save(queue);

        log.info(
            "queue.resumed queueId={} previousStatus={} newStatus={}",
            queueId,
            QueueStatus.PAUSED,
            savedQueue.getStatus()
        );

        return QueueResponse.from(savedQueue);
    }

    @Transactional
    public QueueResponse closeQueue(Long queueId) {

        Queue queue = getQueueEntityForUpdate(queueId);

        if (!queue.getStatus().canTransitionTo(QueueStatus.CLOSED)) {
            throw new ConflictException(
                    "Queue with id " + queueId
                            + " cannot transition from "
                            + queue.getStatus()
                            + " to CLOSED"
            );
        }

        QueueStatus previousStatus = queue.getStatus();

        queue.setStatus(QueueStatus.CLOSED);

        Queue savedQueue = queueRepository.save(queue);

        log.info(
            "queue.closed queueId={} previousStatus={} newStatus={}",
            queueId,
            previousStatus,
            savedQueue.getStatus()
        );

        return QueueResponse.from(savedQueue);
    }

    private Queue getQueueEntity(Long queueId) {

        return queueRepository.findById(queueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Queue with id " + queueId + " was not found"
                        ));
    }

    private Queue getQueueEntityForUpdate(Long queueId) {

        return queueRepository.findByIdForUpdate(queueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Queue with id " + queueId + " was not found"
                        ));
    }
}