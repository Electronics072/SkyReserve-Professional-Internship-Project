package com.skyreserve.repository;

import com.skyreserve.model.Booking;
import com.skyreserve.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserEmailOrderByCreatedAtDesc(String email);
    List<Booking> findByScheduleIdAndStatus(Long scheduleId, BookingStatus status);
    boolean existsByScheduleIdAndSeatNumberAndStatus(Long scheduleId, String seat, BookingStatus status);
    Optional<Booking> findByBookingReference(String ref);
    long countByStatus(BookingStatus status);

    @Query("select coalesce(sum(b.amount), 0) from Booking b where b.status = com.skyreserve.model.BookingStatus.CONFIRMED")
    double confirmedRevenue();
}
