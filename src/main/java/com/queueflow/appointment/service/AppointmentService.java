package com.queueflow.appointment.service;

import com.queueflow.appointment.Appointment;
import com.queueflow.appointment.AppointmentRepository;
import com.queueflow.appointment.AppointmentStatus;
import com.queueflow.appointment.dto.AppointmentResponse;
import com.queueflow.appointment.dto.CreateAppointmentRequest;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.common.exception.ResourceNotFoundException;
import java.util.List;
import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueRepository;
import com.queueflow.queue.QueueStatus;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;
import jakarta.transaction.Transactional;
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

        int nextToken = queue.getCurrentToken() + 1;
        queue.setCurrentToken(nextToken);

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
                        "Appointment with id " + appointmentId
                                + " was not found"
                ));

        if (appointment.getStatus() != AppointmentStatus.WAITING) {
            throw new ConflictException(
                    "Appointment with id " + appointmentId
                            + " cannot be called from status "
                            + appointment.getStatus()
            );
        }

        appointment.setStatus(AppointmentStatus.CALLED);

        return AppointmentResponse.from(
                appointmentRepository.save(appointment)
        );
    }


    @Transactional
    public AppointmentResponse startAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
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
    Appointment appointment = appointmentRepository.findById(appointmentId)
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
    Appointment appointment = appointmentRepository.findById(appointmentId)
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