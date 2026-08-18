package com.skyreserve.controller;

import com.skyreserve.model.Flight;
import com.skyreserve.model.FlightSchedule;
import com.skyreserve.repository.FlightRepository;
import com.skyreserve.repository.FlightScheduleRepository;
import com.skyreserve.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map; import java.util.TreeSet; import java.util.Set;

@Controller
public class HomeController {
    private final FlightRepository flights;
    private final FlightScheduleRepository schedules;
    private final BookingService bookings;

    public HomeController(FlightRepository f, FlightScheduleRepository s, BookingService b) {
        flights = f; schedules = s; bookings = b;
    }

    @GetMapping("/")
    String home(Model m) {
        List<Flight> active = flights.findByActiveTrue();
        m.addAttribute("flights", active);
        Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        active.forEach(f -> { cities.add(f.getSource()); cities.add(f.getDestination()); });
        m.addAttribute("cities", cities);
        return "index";
    }

    @GetMapping("/login")
    String login() { return "login"; }

    @GetMapping("/search")
    String search(@RequestParam String source,
                  @RequestParam String destination,
                  @RequestParam(required = false) LocalDate date,
                  Model m) {
        LocalDate travelDate = date == null ? LocalDate.now().plusDays(1) : date;
        List<Flight> matched = flights.findBySourceIgnoreCaseAndDestinationIgnoreCaseAndActiveTrue(source, destination);
        List<Flight> fs = new java.util.ArrayList<>();
        Map<Long, FlightSchedule> scheduleMap = new LinkedHashMap<>();
        Map<Long, Long> availableMap = new LinkedHashMap<>();
        for (Flight f : matched) {
            FlightSchedule sc = schedules.findByFlightIdAndTravelDate(f.getId(), travelDate)
                    .orElseGet(() -> schedules.save(new FlightSchedule(f, travelDate)));
            if (!"SCHEDULED".equalsIgnoreCase(sc.getStatus())) continue;
            fs.add(f);
            scheduleMap.put(f.getId(), sc);
            availableMap.put(f.getId(), Math.max(0L, f.getTotalSeats() - bookings.occupiedSeats(sc.getId()).size()));
        }
        m.addAttribute("results", fs);
        m.addAttribute("date", travelDate);
        m.addAttribute("scheduleMap", scheduleMap);
        m.addAttribute("availableMap", availableMap);
        return "search";
    }
}
