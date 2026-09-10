package com.queueflow.appointment.service;

import com.queueflow.appointment.Appointment;
import com.queueflow.appointment.AppointmentRepository;
import com.queueflow.appointment.AppointmentStatus;
import com.queueflow.appointment.dto.AppointmentResponse;
import com.queueflow.appointment.dto.CreateAppointmentRequest;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.appointment.dto.QueuePositionResponse;
import java.util.List;
import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueRepository;
import com.queueflow.queue.QueueStatus;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final QueueRepository queueRepository;
    private final UserRepository userRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            QueueRepository queueRepository,
            UserRepository userRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.queueRepository = queueRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AppointmentResponse createAppointment(
            CreateAppointmentRequest request
    ) {
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer with id " + request.getCustomerId()
                                + " was not found"
                ));

        Queue queue = queueRepository.findByIdForUpdate(request.getQueueId())
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
        appointment.setCustomer(customer);
        appointment.setBusiness(queue.getBusiness());
        appointment.setService(queue.getService());
        appointment.setQueue(queue);
        appointment.setTokenNumber(nextToken);
        appointment.setAppointmentDate(queue.getQueueDate());
        appointment.setStatus(AppointmentStatus.WAITING);

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        return AppointmentResponse.from(savedAppointment);
    }

    @Transactional
public AppointmentResponse callAppointment(Long appointmentId) {
    Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Appointment with id " + appointmentId + " was not found"));

    Long queueId = appointment.getQueue().getId();

    Queue queue = queueRepository.findByIdForUpdate(queueId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Queue with id " + queueId + " was not found"));

    Appointment lockedAppointment = appointmentRepository.findByIdForUpdate(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Appointment with id " + appointmentId + " was not found"));

    if (lockedAppointment.getStatus() != AppointmentStatus.WAITING) {
        throw new ConflictException(
                "Appointment with id " + appointmentId
                        + " cannot transition from "
                        + lockedAppointment.getStatus() + " to CALLED");
    }

    if (queue.getStatus() != QueueStatus.ACTIVE) {
        throw new ConflictException(
                "Queue with id " + queueId + " is not active");
    }

    lockedAppointment.setStatus(AppointmentStatus.CALLED);
    queue.setCurrentToken(lockedAppointment.getTokenNumber());

    appointmentRepository.save(lockedAppointment);
    queueRepository.save(queue);

    return AppointmentResponse.from(lockedAppointment);
}


    @Transactional
    public AppointmentResponse startAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment with id " + appointmentId
                                + " was not found"
                ));

        if (appointment.getStatus() != AppointmentStatus.CALLED) {
            throw new ConflictException(
                    "Appointment with id " + appointmentId
                            + " cannot be started from status "
                            + appointment.getStatus()
            );
        }

        appointment.setStatus(AppointmentStatus.IN_SERVICE);

        return AppointmentResponse.from(
                appointmentRepository.save(appointment)
        );
    }

    @Transactional
public AppointmentResponse completeAppointment(Long appointmentId) {
    Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Appointment with id " + appointmentId
                            + " was not found"
            ));

    if (appointment.getStatus() != AppointmentStatus.IN_SERVICE) {
        throw new ConflictException(
                "Appointment with id " + appointmentId
                        + " cannot be completed from status "
                        + appointment.getStatus()
        );
    }

    appointment.setStatus(AppointmentStatus.COMPLETED);

    return AppointmentResponse.from(
            appointmentRepository.save(appointment)
    );
}

@Transactional
public AppointmentResponse cancelAppointment(Long appointmentId) {
    Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Appointment with id " + appointmentId
                            + " was not found"
            ));

    AppointmentStatus currentStatus = appointment.getStatus();

    if (currentStatus != AppointmentStatus.WAITING
            && currentStatus != AppointmentStatus.CALLED) {
        throw new ConflictException(
                "Appointment with id " + appointmentId
                        + " cannot be cancelled from status "
                        + currentStatus
        );
    }

    appointment.setStatus(AppointmentStatus.CANCELLED);

    return AppointmentResponse.from(
            appointmentRepository.save(appointment)
    );
}

@Transactional
public AppointmentResponse markNoShow(Long appointmentId) {

    Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Appointment with id " + appointmentId
                            + " was not found"
            ));

    if (appointment.getStatus() != AppointmentStatus.CALLED) {
        throw new ConflictException(
                "Appointment with id " + appointmentId
                        + " cannot be marked as NO_SHOW from status "
                        + appointment.getStatus()
        );
    }

    appointment.setStatus(AppointmentStatus.NO_SHOW);

    return AppointmentResponse.from(
            appointmentRepository.save(appointment)
    );
}

@Transactional(readOnly = true)
public QueuePositionResponse getQueuePosition(Long appointmentId) {

    Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Appointment with id " + appointmentId
                            + " was not found"
            ));

    AppointmentStatus status = appointment.getStatus();

    // Only WAITING appointments have a position in the queue.
    if (status != AppointmentStatus.WAITING) {
        return new QueuePositionResponse(
                appointment.getId(),
                appointment.getTokenNumber(),
                status,
                0,
                0
        );
    }

    long appointmentsAhead =
            appointmentRepository
                    .countByQueueIdAndStatusAndTokenNumberLessThan(
                            appointment.getQueue().getId(),
                            AppointmentStatus.WAITING,
                            appointment.getTokenNumber()
                    );

    int queuePosition = Math.toIntExact(appointmentsAhead);

    int durationMinutes =
            appointment.getService().getDurationMinutes();

    int estimatedWaitMinutes =
            queuePosition * durationMinutes;

    return new QueuePositionResponse(
            appointment.getId(),
            appointment.getTokenNumber(),
            status,
            queuePosition,
            estimatedWaitMinutes
    );
}

@Transactional(readOnly = true)
public AppointmentResponse getAppointment(Long appointmentId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Appointment with id " + appointmentId
                            + " was not found"
            ));

    return AppointmentResponse.from(appointment);
}

@Transactional
public List<AppointmentResponse> getCustomerAppointments(
        Long customerId
) {
    User customer = userRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Customer with id " + customerId
                            + " was not found"
            ));

    if (customer.getRole() != UserRole.CUSTOMER) {
        throw new ConflictException(
                "User with id " + customerId
                        + " is not a customer"
        );
    }

    return appointmentRepository
            .findByCustomerIdOrderByCreatedAtDesc(customerId)
            .stream()
            .map(AppointmentResponse::from)
            .toList();
}


}