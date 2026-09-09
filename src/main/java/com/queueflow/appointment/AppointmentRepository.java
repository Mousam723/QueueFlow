package com.queueflow.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByQueueIdOrderByTokenNumberAsc(Long queueId);

    List<Appointment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Appointment> findByQueueIdAndStatusOrderByTokenNumberAsc(
            Long queueId,
            AppointmentStatus status
    );
}