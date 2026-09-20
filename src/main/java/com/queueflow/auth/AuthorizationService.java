package com.queueflow.auth;

import com.queueflow.business.Business;
import com.queueflow.business.BusinessRepository;
import com.queueflow.businessstaff.BusinessStaffRepository;
import com.queueflow.common.exception.ResourceNotFoundException;
import com.queueflow.user.User;
import com.queueflow.user.UserRepository;
import com.queueflow.user.UserRole;
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

    public AuthorizationService(
            UserRepository userRepository,
            BusinessRepository businessRepository,
            BusinessStaffRepository businessStaffRepository
    ) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.businessStaffRepository = businessStaffRepository;
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
}