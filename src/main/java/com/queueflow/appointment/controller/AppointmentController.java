package com.queueflow.appointment.controller;

import com.queueflow.appointment.dto.AppointmentResponse;
import com.queueflow.appointment.dto.CreateAppointmentRequest;
import com.queueflow.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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
    public AppointmentResponse createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request
    ) {
        return appointmentService.createAppointment(request);
    }

    @PostMapping("/{appointmentId}/call")
    @ResponseStatus(HttpStatus.OK)
    public AppointmentResponse callAppointment(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.callAppointment(appointmentId);
    }


    @PostMapping("/{appointmentId}/start")
    @ResponseStatus(HttpStatus.OK)
    public AppointmentResponse startAppointment(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.startAppointment(appointmentId);
    }

    @PostMapping("/{appointmentId}/complete")
@ResponseStatus(HttpStatus.OK)
public AppointmentResponse completeAppointment(
        @PathVariable Long appointmentId
) {
    return appointmentService.completeAppointment(appointmentId);
}

@PostMapping("/{appointmentId}/cancel")
@ResponseStatus(HttpStatus.OK)
public AppointmentResponse cancelAppointment(
        @PathVariable Long appointmentId
) {
    return appointmentService.cancelAppointment(appointmentId);
}

@GetMapping("/{appointmentId}")
@ResponseStatus(HttpStatus.OK)
public AppointmentResponse getAppointment(
        @PathVariable Long appointmentId
) {
    return appointmentService.getAppointment(appointmentId);
}

@GetMapping("/customers/{customerId}")
@ResponseStatus(HttpStatus.OK)
public List<AppointmentResponse> getCustomerAppointments(
        @PathVariable Long customerId
) {
    return appointmentService.getCustomerAppointments(customerId);
}

}