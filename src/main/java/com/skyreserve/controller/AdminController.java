package com.skyreserve.controller;

import com.skyreserve.model.*;
import com.skyreserve.repository.FlightRepository;
import com.skyreserve.repository.FlightScheduleRepository;
import com.skyreserve.service.BookingService;
import com.skyreserve.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final FlightRepository flights;
    private final FlightScheduleRepository schedules;
    private final UserService users;
    private final BookingService bookings;

    public AdminController(FlightRepository f, FlightScheduleRepository s, UserService u, BookingService b) {
        flights = f; schedules = s; users = u; bookings = b;
    }

    @GetMapping
    String dashboard(Model m) {
        List<Booking> all = bookings.all();
        Map<String, Long> flightCounts = all.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.groupingBy(b -> b.getSchedule().getFlight().getFlightNumber(), Collectors.counting()));
        Map<String, Double> routeRevenue = all.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.groupingBy(
                        b -> b.getSchedule().getFlight().getSource() + " → " + b.getSchedule().getFlight().getDestination(),
                        Collectors.summingDouble(Booking::getAmount)));

        m.addAttribute("flightCount", flights.count());
        m.addAttribute("activeFlightCount", flights.findByActiveTrue().size());
        m.addAttribute("userCount", users.count());
        m.addAttribute("bookingCount", bookings.count());
        m.addAttribute("confirmed", bookings.confirmed());
        m.addAttribute("cancelled", bookings.cancelled());
        m.addAttribute("revenue", bookings.revenue());
        m.addAttribute("flights", flights.findAll());
        m.addAttribute("schedules", schedules.findByTravelDateGreaterThanEqualOrderByTravelDateAsc(LocalDate.now()));
        m.addAttribute("users", users.all());
        m.addAttribute("flightCounts", flightCounts);
        m.addAttribute("routeRevenue", routeRevenue);
        return "admin";
    }

    @PostMapping("/flights")
    String add(@RequestParam String flightNumber,
               @RequestParam String airline,
               @RequestParam String source,
               @RequestParam String destination,
               @RequestParam String departureTime,
               @RequestParam String arrivalTime,
               @RequestParam double economyPrice,
               @RequestParam double businessPrice,
               @RequestParam int totalSeats,
               RedirectAttributes ra) {
        String n=flightNumber.trim(), a=airline.trim(), s=source.trim(), d=destination.trim();
        if(n.isBlank() || a.isBlank() || s.isBlank() || d.isBlank() || departureTime.isBlank() || arrivalTime.isBlank()) {
            ra.addFlashAttribute("adminError", "All flight details are required: number, airline, source, destination, departure, arrival, fares and seats.");
            return "redirect:/admin";
        }
        if(s.equalsIgnoreCase(d)) {
            ra.addFlashAttribute("adminError", "Source and destination must be different.");
            return "redirect:/admin";
        }
        if(flights.findByFlightNumberIgnoreCase(n).isPresent()) {
            ra.addFlashAttribute("adminError", "Flight number " + n + " already exists.");
            return "redirect:/admin";
        }
        if(economyPrice <= 0 || businessPrice <= 0 || totalSeats < 6 || totalSeats > 180) {
            ra.addFlashAttribute("adminError", "Enter valid fares greater than zero and seats between 6 and 180.");
            return "redirect:/admin";
        }
        if(!departureTime.matches("\\d{2}:\\d{2}") || !arrivalTime.matches("\\d{2}:\\d{2}")) {
            ra.addFlashAttribute("adminError", "Departure and arrival times must use HH:MM format.");
            return "redirect:/admin";
        }
        flights.save(new Flight(n, a, s, d, departureTime.trim(), arrivalTime.trim(), economyPrice, businessPrice, totalSeats));
        ra.addFlashAttribute("adminSuccess", "Flight " + n + " added successfully.");
        return "redirect:/admin";
    }

    @PostMapping("/flights/{id}/toggle")
    String toggle(@PathVariable Long id) {
        Flight f = flights.findById(id).orElseThrow();
        f.setActive(!f.isActive());
        flights.save(f);
        return "redirect:/admin";
    }

    @PostMapping("/schedules")
    String addSchedule(@RequestParam Long flightId, @RequestParam LocalDate travelDate) {
        Flight flight = flights.findById(flightId).orElseThrow();
        if (schedules.findByFlightIdAndTravelDate(flightId, travelDate).isEmpty()) {
            schedules.save(new FlightSchedule(flight, travelDate));
        }
        return "redirect:/admin";
    }

    @PostMapping("/schedules/{id}/status")
    String updateScheduleStatus(@PathVariable Long id, @RequestParam String status) {
        FlightSchedule s = schedules.findById(id).orElseThrow();
        s.setStatus(status.toUpperCase(Locale.ROOT));
        schedules.save(s);
        return "redirect:/admin";
    }
}
