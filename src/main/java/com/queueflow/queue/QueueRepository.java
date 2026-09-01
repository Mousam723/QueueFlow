package com.queueflow.queue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QueueRepository extends JpaRepository<Queue, Long> {

    Optional<Queue> findByServiceIdAndQueueDate(
            Long serviceId,
            LocalDate queueDate
    );

    List<Queue> findByBusinessIdAndQueueDate(
            Long businessId,
            LocalDate queueDate
    );

}