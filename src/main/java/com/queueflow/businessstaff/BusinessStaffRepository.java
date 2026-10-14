package com.queueflow.businessstaff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessStaffRepository
        extends JpaRepository<BusinessStaff, Long> {

    boolean existsByBusinessIdAndStaffId(
            Long businessId,
            Long staffId
    );

    Page<BusinessStaff> findByBusinessIdOrderByCreatedAtAsc(
            Long businessId,
            Pageable pageable
    );

    Page<BusinessStaff> findByStaffIdOrderByCreatedAtAsc(
            Long staffId,
            Pageable pageable
    );
}
