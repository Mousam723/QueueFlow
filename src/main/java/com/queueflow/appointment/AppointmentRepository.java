package com.queueflow.appointment;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);
    List<Appointment> findByQueueIdOrderByTokenNumberAsc(Long queueId);

    Page<Appointment> findByCustomerIdOrderByCreatedAtDescIdDesc(
            Long customerId,
            Pageable pageable
    );

    List<Appointment> findByQueueIdAndStatusOrderByTokenNumberAsc(
            Long queueId,
            AppointmentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :appointmentId")
    Optional<Appointment> findByIdForUpdate(
            @Param("appointmentId") Long appointmentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.queue.id = :queueId
              AND a.status = :status
            ORDER BY a.tokenNumber ASC
            """)
    List<Appointment> findByQueueIdAndStatusForUpdate(
            @Param("queueId") Long queueId,
            @Param("status") AppointmentStatus status
    );

    @Query("""
            SELECT COUNT(a)
            FROM Appointment a
            WHERE a.queue.id = :queueId
              AND a.status = :status
              AND a.tokenNumber < :tokenNumber
            """)
    long countByQueueIdAndStatusAndTokenNumberLessThan(
            @Param("queueId") Long queueId,
            @Param("status") AppointmentStatus status,
            @Param("tokenNumber") Integer tokenNumber
    );
}