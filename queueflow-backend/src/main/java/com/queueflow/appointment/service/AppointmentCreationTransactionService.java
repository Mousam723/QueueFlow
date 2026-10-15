package com.queueflow.appointment.service;

import com.queueflow.appointment.Appointment;
import com.queueflow.appointment.AppointmentRepository;
import com.queueflow.appointment.AppointmentStatus;
import com.queueflow.appointment.dto.CreateAppointmentRequest;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueRepository;
import com.queueflow.queue.QueueStatus;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentCreationTransactionService {

    private final AppointmentRepository appointmentRepository;
    private final QueueRepository queueRepository;
    private final UserRepository userRepository;

    public AppointmentCreationTransactionService(
            AppointmentRepository appointmentRepository,
            QueueRepository queueRepository,
            UserRepository userRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.queueRepository = queueRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Appointment createAppointment(
            CreateAppointmentRequest request,
            String idempotencyKey,
            String requestFingerprint
    ) {
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer with id " + request.getCustomerId()
                                + " was not found"
                ));

        Queue queue = queueRepository.findByIdForUpdate(
                        request.getQueueId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue with id " + request.getQueueId()
                                + " was not found"
                ));

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new ConflictException(
                    "User with id " + customer.getId()
                            + " is not a customer"
            );
        }

        if (queue.getStatus() != QueueStatus.ACTIVE) {
            throw new ConflictException(
                    "Queue with id " + queue.getId()
                            + " is not active"
            );
        }

        int nextToken = queue.getNextTokenNumber() + 1;
        queue.setNextTokenNumber(nextToken);
        queueRepository.save(queue);

        Appointment appointment = new Appointment();

        appointment.setIdempotencyKey(idempotencyKey);
        appointment.setRequestFingerprint(requestFingerprint);
        appointment.setCustomer(customer);
        appointment.setBusiness(queue.getBusiness());
        appointment.setService(queue.getService());
        appointment.setQueue(queue);
        appointment.setTokenNumber(nextToken);
        appointment.setAppointmentDate(queue.getQueueDate());
        appointment.setStatus(AppointmentStatus.WAITING);

        return appointmentRepository.save(appointment);
    }
}
