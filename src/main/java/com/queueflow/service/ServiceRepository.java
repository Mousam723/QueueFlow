package com.queueflow.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    Page<Service> findByBusinessIdOrderByIdAsc(
            Long businessId,
            Pageable pageable
    );
}
