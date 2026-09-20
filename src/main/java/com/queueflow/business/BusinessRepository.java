package com.queueflow.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    Page<Business> findByOwnerIdOrderByIdAsc(Long ownerId, Pageable pageable);
}
