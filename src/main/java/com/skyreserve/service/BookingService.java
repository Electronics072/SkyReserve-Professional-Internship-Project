package com.skyreserve.service;

import com.skyreserve.model.*;
import com.skyreserve.repository.BookingRepository;
import com.skyreserve.repository.FlightScheduleRepository;
import com.skyreserve.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final FlightScheduleRepository schedules;
    private final UserRepository users;

    public BookingService(BookingRepository b, FlightScheduleRepository s, UserRepository u) {
        bookings = b;
        schedules = s;
        users = u;
    }

    @Transactional
    public Booking create(Long scheduleId, String email, String passenger, String seat, String seatClass, String paymentMethod) {
        FlightSchedule sc = schedules.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        if (!sc.getFlight().isActive() || !"SCHEDULED".equalsIgnoreCase(sc.getStatus())) {
            throw new IllegalStateException("This flight schedule is not available for booking.");
        }
        String normalizedSeat = seat.trim().toUpperCase(Locale.ROOT);
        if (bookings.existsByScheduleIdAndSeatNumberAndStatus(scheduleId, normalizedSeat, BookingStatus.CONFIRMED)) {
            throw new IllegalStateException("Seat " + normalizedSeat + " is no longer available. Please choose another seat.");
        }
        User user = users.findByEmail(email).orElseThrow();
        Booking b = new Booking();
        b.setBookingReference("SR" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        b.setUser(user);
        b.setSchedule(sc);
        b.setPassengerName(passenger.trim());
        b.setSeatNumber(normalizedSeat);
        b.setSeatClass(seatClass);
        b.setPaymentMethod(paymentMethod == null || paymentMethod.isBlank() ? "Demo UPI" : paymentMethod.trim());
        b.setPaymentStatus("DEMO_PAID");
        double baseFare = "Business".equalsIgnoreCase(seatClass)
                ? sc.getFlight().getBusinessPrice()
                : sc.getFlight().getEconomyPrice();
        double serviceFee = Math.round(baseFare * 0.05 * 100.0) / 100.0;
        b.setAmount(baseFare + serviceFee);
        try {
            return bookings.saveAndFlush(b);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Seat " + normalizedSeat + " was booked by another user. Please choose another seat.");
        }
    }

    public List<Booking> mine(String email) {
        return bookings.findByUserEmailOrderByCreatedAtDesc(email);
    }

    public Optional<Booking> byRef(String ref) {
        return bookings.findByBookingReference(ref);
    }

    public Set<String> occupiedSeats(Long scheduleId) {
        Set<String> result = new HashSet<>();
        bookings.findByScheduleIdAndStatus(scheduleId, BookingStatus.CONFIRMED)
                .forEach(b -> result.add(b.getSeatNumber()));
        return result;
    }

    public long count() { return bookings.count(); }
    public long confirmed() { return bookings.countByStatus(BookingStatus.CONFIRMED); }
    public long cancelled() { return bookings.countByStatus(BookingStatus.CANCELLED); }
    public double revenue() { return bookings.confirmedRevenue(); }
    public List<Booking> all() { return bookings.findAll(); }

    @Transactional
    public void cancel(String ref, String email, boolean admin) {
        Booking b = bookings.findByBookingReference(ref).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!admin && !b.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new SecurityException("You are not allowed to cancel this booking.");
        }
        if (b.getStatus() == BookingStatus.CANCELLED) return;
        b.setStatus(BookingStatus.CANCELLED);
        bookings.save(b);
    }
}
