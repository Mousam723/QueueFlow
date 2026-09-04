package com.queueflow.queue;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Queue q WHERE q.id = :queueId")
    Optional<Queue> findByIdForUpdate(
            @Param("queueId") Long queueId
    );
}
