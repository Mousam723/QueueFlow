package com.queueflow.auth;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.businessstaff.BusinessStaffRepository;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;
import com.queueflow.queue.Queue;
import com.queueflow.queue.QueueRepository;
import com.queueflow.appointment.Appointment;
import com.queueflow.appointment.AppointmentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    @Value("${queueflow.security.enabled:true}")
    private boolean securityEnabled;

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessStaffRepository businessStaffRepository;
    private final QueueRepository queueRepository;
    private final AppointmentRepository appointmentRepository;

    public AuthorizationService(
            UserRepository userRepository,
            BusinessRepository businessRepository,
            BusinessStaffRepository businessStaffRepository,
            QueueRepository queueRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.businessStaffRepository = businessStaffRepository;
        this.queueRepository = queueRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Long getAuthenticatedUserId() {
        if (!securityEnabled) {
            return null;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user was not found"
                        )
                );

        return user.getId();
    }

    public boolean isAdmin() {
        if (!securityEnabled) {
            return true;
        }

        return hasRole(UserRole.ADMIN);
    }

    public boolean isBusinessOwner(Long businessId) {
        if (!securityEnabled) {
            return true;
        }

        Long userId = getAuthenticatedUserId();

        if (userId == null) {
            return false;
        }

        return businessRepository.findById(businessId)
                .map(Business::getOwner)
                .map(User::getId)
                .map(ownerId -> ownerId.equals(userId))
                .orElse(false);
    }

    public boolean isBusinessOwnerId(Long ownerId) {
        if (!securityEnabled) {
            return true;
        }

        Long userId = getAuthenticatedUserId();

        if (userId == null || ownerId == null) {
            return false;
        }

        return userId.equals(ownerId);
    }

    public boolean canCreateBusiness(Long ownerId) {
        if (!securityEnabled) {
            return true;
        }

        if (isAdmin()) {
            return true;
        }

        return hasRole(UserRole.BUSINESS_OWNER)
                && isBusinessOwnerId(ownerId);
    }

    public boolean isStaffAssignedToBusiness(Long businessId) {
        if (!securityEnabled) {
            return true;
        }

        Long userId = getAuthenticatedUserId();

        if (userId == null) {
            return false;
        }

        return businessStaffRepository
                .existsByBusinessIdAndStaffId(
                        businessId,
                        userId
                );
    }

    public boolean canAccessBusiness(Long businessId) {
        if (!securityEnabled) {
            return true;
        }

        if (isAdmin()) {
            return true;
        }

        return isBusinessOwner(businessId)
                || isStaffAssignedToBusiness(businessId);
    }

    private boolean hasRole(UserRole role) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_" + role.name())
                );
    }

    public boolean canAccessQueue(Long queueId) {
        if (!securityEnabled) {
            return true;
        }

        Queue queue = queueRepository.findById(queueId).orElse(null);

        if (queue == null) {
            return false;
        }

        return canAccessBusiness(queue.getBusiness().getId());
    }

    public boolean canAccessAppointment(Long appointmentId) {
        if (!securityEnabled) {
            return true;
        }

        Appointment appointment = appointmentRepository
                .findById(appointmentId)
                .orElse(null);

        if (appointment == null) {
            return false;
        }

        if (isAdmin()) {
            return true;
        }

        if (isBusinessOwner(appointment.getBusiness().getId())) {
            return true;
        }

        if (isStaffAssignedToBusiness(appointment.getBusiness().getId())) {
            return true;
        }

        Long authenticatedUserId = getAuthenticatedUserId();

        return authenticatedUserId != null
                && appointment.getCustomer().getId().equals(authenticatedUserId);
    }

    public boolean canManageAppointment(Long appointmentId) {
        if (!securityEnabled) {
            return true;
        }

        Appointment appointment = appointmentRepository
                .findById(appointmentId)
                .orElse(null);

        if (appointment == null) {
            return false;
        }

        if (isAdmin()) {
            return true;
        }

        if (isBusinessOwner(appointment.getBusiness().getId())) {
            return true;
        }

        return isStaffAssignedToBusiness(
                appointment.getBusiness().getId()
        );
    }

    public boolean canAccessCustomerAppointments(Long customerId) {
        if (!securityEnabled) {
            return true;
        }

        if (isAdmin()) {
            return true;
        }

        Long authenticatedUserId = getAuthenticatedUserId();

        if (authenticatedUserId == null || customerId == null) {
            return false;
        }

        return authenticatedUserId.equals(customerId);
    }

    public boolean canCreateAppointment(
            Long customerId,
            Long queueId
    ) {
        if (!securityEnabled) {
            return true;
        }

        if (isAdmin()) {
            return true;
        }

        Long authenticatedUserId = getAuthenticatedUserId();

        if (authenticatedUserId == null || customerId == null) {
            return false;
        }

        if (hasRole(UserRole.CUSTOMER)) {
            return authenticatedUserId.equals(customerId);
        }

        if (hasRole(UserRole.BUSINESS_OWNER)) {
            return canAccessQueue(queueId);
        }

        return false;
    }
}