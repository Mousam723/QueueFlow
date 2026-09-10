package com.queueflow.businessstaff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessStaffRepository
        extends JpaRepository<BusinessStaff, Long> {

    boolean existsByBusinessIdAndStaffId(
            Long businessId,
            Long staffId
    );

    List<BusinessStaff> findByBusinessIdOrderByCreatedAtAsc(
            Long businessId
    );
}
