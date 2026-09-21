package com.queueflow.appointment.controller;

import com.queueflow.appointment.dto.AppointmentResponse;
import com.queueflow.appointment.dto.CreateAppointmentRequest;
import com.queueflow.appointment.dto.QueuePositionResponse;
import com.queueflow.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("@authorizationService.canCreateAppointment(#request.customerId, #request.queueId)")
public AppointmentResponse createAppointment(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CreateAppointmentRequest request
) {
    return appointmentService.createAppointment(request, idempotencyKey);
}

    @PostMapping("/{appointmentId}/call")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@authorizationService.canManageAppointment(#appointmentId)")
    public AppointmentResponse callAppointment(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.callAppointment(appointmentId);
    }


    @PostMapping("/{appointmentId}/start")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@authorizationService.canManageAppointment(#appointmentId)")
    public AppointmentResponse startAppointment(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.startAppointment(appointmentId);
    }

    @PostMapping("/{appointmentId}/complete")
@ResponseStatus(HttpStatus.OK)
@PreAuthorize("@authorizationService.canManageAppointment(#appointmentId)")
public AppointmentResponse completeAppointment(
        @PathVariable Long appointmentId
) {
    return appointmentService.completeAppointment(appointmentId);
}

@PostMapping("/{appointmentId}/cancel")
@ResponseStatus(HttpStatus.OK)
@PreAuthorize("@authorizationService.canAccessAppointment(#appointmentId)")
public AppointmentResponse cancelAppointment(
        @PathVariable Long appointmentId
) {
    return appointmentService.cancelAppointment(appointmentId);
}

@PostMapping("/{appointmentId}/no-show")
@ResponseStatus(HttpStatus.OK)
@PreAuthorize("@authorizationService.canManageAppointment(#appointmentId)")
public AppointmentResponse markNoShow(
        @PathVariable Long appointmentId
) {
    return appointmentService.markNoShow(appointmentId);
}

@GetMapping("/{appointmentId}/position")
@ResponseStatus(HttpStatus.OK)
@PreAuthorize("@authorizationService.canAccessAppointment(#appointmentId)")
public QueuePositionResponse getQueuePosition(
        @PathVariable Long appointmentId
) {
    return appointmentService.getQueuePosition(appointmentId);
}

@GetMapping("/{appointmentId}")
@ResponseStatus(HttpStatus.OK)
@PreAuthorize("@authorizationService.canAccessAppointment(#appointmentId)")
public AppointmentResponse getAppointment(
        @PathVariable Long appointmentId
) {
    return appointmentService.getAppointment(appointmentId);
}

@GetMapping("/customers/{customerId}")
@ResponseStatus(HttpStatus.OK)
@PreAuthorize("@authorizationService.canAccessCustomerAppointments(#customerId)")
public Page<AppointmentResponse> getCustomerAppointments(
        @PathVariable Long customerId,
        Pageable pageable
) {
    return appointmentService.getCustomerAppointments(
            customerId,
            pageable
    );
}

}