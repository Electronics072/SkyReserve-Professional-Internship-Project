package com.skyreserve.repository;

import com.skyreserve.model.FlightSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, Long> {
    Optional<FlightSchedule> findByFlightIdAndTravelDate(Long flightId, LocalDate date);
    List<FlightSchedule> findByTravelDateGreaterThanEqualOrderByTravelDateAsc(LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FlightSchedule s where s.id = :id")
    Optional<FlightSchedule> findByIdForUpdate(@Param("id") Long id);
}
