package com.queueflow.appointment.service;

import com.queueflow.appointment.Appointment;
import com.queueflow.appointment.AppointmentRepository;
import com.queueflow.appointment.AppointmentStatus;
import com.queueflow.appointment.dto.AppointmentResponse;
import com.queueflow.appointment.dto.CreateAppointmentRequest;
import com.queueflow.common.exception.ConflictException;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.appointment.dto.QueuePositionResponse;
import com.queueflow.common.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueRepository;
import com.queueflow.queue.QueueStatus;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {
    private static final Logger log =
        LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final AppointmentCreationTransactionService appointmentCreationTransactionService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            QueueRepository queueRepository,
            UserRepository userRepository,
            AppointmentCreationTransactionService
                appointmentCreationTransactionService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.queueRepository = queueRepository;
        this.userRepository = userRepository;
        this.appointmentCreationTransactionService =
                appointmentCreationTransactionService;
    }

    public AppointmentResponse createAppointment(
        CreateAppointmentRequest request
) {
    return createAppointment(
            request,
            "legacy-" + java.util.UUID.randomUUID()
    );
}

public AppointmentResponse createAppointment(
        CreateAppointmentRequest request,
        String idempotencyKey
) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
        throw new BadRequestException(
                "Idempotency-Key header must not be blank"
        );
    }

    if (idempotencyKey.length() > 100) {
        throw new BadRequestException(
                "Idempotency-Key header must not exceed 100 characters"
        );
    }



    String requestFingerprint =
            generateRequestFingerprint(request);

    Appointment existingAppointment =
            appointmentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElse(null);

    if (existingAppointment != null) {
        return resolveExistingAppointment(
                existingAppointment,
                requestFingerprint
        );
    }

    try {
        Appointment createdAppointment =
        appointmentCreationTransactionService.createAppointment(
                request,
                idempotencyKey,
                requestFingerprint
        );

        log.info(
            "appointment.created appointmentId={} customerId={} queueId={} tokenNumber={}",
            createdAppointment.getId(),
            request.getCustomerId(),
            request.getQueueId(),
            createdAppointment.getTokenNumber()
        );

        return AppointmentResponse.from(createdAppointment);

    } catch (DataIntegrityViolationException exception) {

        /*
         * Another transaction may have created the same
         * idempotency key concurrently.
         *
         * The transaction that attempted the duplicate insert
         * has already rolled back before this catch block runs.
         */
        Appointment concurrentAppointment =
                appointmentRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (concurrentAppointment == null) {
            throw exception;
        }

        return resolveExistingAppointment(
                concurrentAppointment,
                requestFingerprint
        );
    }
}

private AppointmentResponse resolveExistingAppointment(
    Appointment existingAppointment,
    String requestFingerprint
) {
if (!requestFingerprint.equals(
        existingAppointment.getRequestFingerprint()
)) {
    throw new ConflictException(
            "Idempotency-Key has already been used "
                    + "with a different request payload"
    );
}

return AppointmentResponse.from(existingAppointment);
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

    log.info(
        "appointment.called appointmentId={} queueId={} tokenNumber={}",
        lockedAppointment.getId(),
        queueId,
        lockedAppointment.getTokenNumber()
    );

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

        Appointment savedAppointment =
            appointmentRepository.save(appointment);

        log.info(
            "appointment.started appointmentId={} tokenNumber={}",
            savedAppointment.getId(),
            savedAppointment.getTokenNumber()
        );

        return AppointmentResponse.from(savedAppointment);
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

    Appointment savedAppointment =
        appointmentRepository.save(appointment);

    log.info(
        "appointment.completed appointmentId={} tokenNumber={}",
        savedAppointment.getId(),
        savedAppointment.getTokenNumber()
    );

    return AppointmentResponse.from(savedAppointment);
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

    Appointment savedAppointment =
        appointmentRepository.save(appointment);

    log.info(
        "appointment.cancelled appointmentId={} previousStatus={} newStatus={}",
        savedAppointment.getId(),
        currentStatus,
        savedAppointment.getStatus()
    );

    return AppointmentResponse.from(savedAppointment);
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

    AppointmentStatus previousStatus = appointment.getStatus();

    appointment.setStatus(AppointmentStatus.NO_SHOW);

    Appointment savedAppointment =
        appointmentRepository.save(appointment);

    log.info(
        "appointment.no_show appointmentId={} previousStatus={} newStatus={}",
        savedAppointment.getId(),
        previousStatus,
        savedAppointment.getStatus()
    );

    return AppointmentResponse.from(savedAppointment);
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

private String generateRequestFingerprint(
    CreateAppointmentRequest request
) {
String payload =
        request.getCustomerId() + ":" + request.getQueueId();

try {
    MessageDigest digest =
            MessageDigest.getInstance("SHA-256");

    byte[] hash =
            digest.digest(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

    StringBuilder hex = new StringBuilder();

    for (byte b : hash) {
        hex.append(String.format("%02x", b));
    }

    return hex.toString();

} catch (NoSuchAlgorithmException e) {
    throw new IllegalStateException(
            "SHA-256 algorithm is not available",
            e
    );
}
}

}