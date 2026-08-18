package com.skyreserve.controller;

import com.skyreserve.model.*;
import com.skyreserve.repository.FlightRepository;
import com.skyreserve.repository.FlightScheduleRepository;
import com.skyreserve.service.BookingService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingController {
    private final FlightRepository flights;
    private final FlightScheduleRepository schedules;
    private final BookingService service;

    public BookingController(FlightRepository f, FlightScheduleRepository s, BookingService b) {
        flights = f; schedules = s; service = b;
    }

    @GetMapping("/new")
    String form(@RequestParam Long flightId, @RequestParam(required = false) LocalDate date, Model m) {
        Flight f = flights.findById(flightId).orElseThrow();
        LocalDate d = date == null ? LocalDate.now().plusDays(1) : date;
        FlightSchedule sc = schedules.findByFlightIdAndTravelDate(flightId, d)
                .orElseGet(() -> schedules.save(new FlightSchedule(f, d)));

        List<String> seats = generateSeats(f.getTotalSeats());
        m.addAttribute("schedule", sc);
        m.addAttribute("flight", f);
        m.addAttribute("seats", seats);
        m.addAttribute("occupiedSeats", service.occupiedSeats(sc.getId()));
        return "booking";
    }

    @PostMapping("/checkout")
    String checkout(@RequestParam Long scheduleId,
                    @RequestParam String passengerName,
                    @RequestParam String seatNumber,
                    @RequestParam String seatClass,
                    Model m) {
        if (passengerName == null || passengerName.trim().isEmpty()) {
            m.addAttribute("error", "Passenger name is required.");
            return "error";
        }
        FlightSchedule sc = schedules.findById(scheduleId).orElseThrow();
        m.addAttribute("schedule", sc);
        m.addAttribute("flight", sc.getFlight());
        m.addAttribute("passengerName", passengerName.trim());
        m.addAttribute("seatNumber", seatNumber);
        m.addAttribute("seatClass", seatClass);
        double fare = "Business".equalsIgnoreCase(seatClass) ? sc.getFlight().getBusinessPrice() : sc.getFlight().getEconomyPrice();
        double serviceFee = Math.round(fare * 0.05 * 100.0) / 100.0;
        m.addAttribute("fare", fare);
        m.addAttribute("serviceFee", serviceFee);
        m.addAttribute("total", fare + serviceFee);
        return "payment";
    }

    @PostMapping("/create")
    String create(@RequestParam Long scheduleId,
                  @RequestParam String passengerName,
                  @RequestParam String seatNumber,
                  @RequestParam String seatClass,
                  @RequestParam String paymentMethod,
                  Authentication auth,
                  Model m) {
        try {
            Booking b = service.create(scheduleId, auth.getName(), passengerName, seatNumber, seatClass, paymentMethod);
            return "redirect:/bookings/" + b.getBookingReference();
        } catch (Exception e) {
            m.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @GetMapping
    String mine(Authentication a, Model m) {
        m.addAttribute("bookings", service.mine(a.getName()));
        return "bookings";
    }

    @GetMapping("/{ref}")
    String view(@PathVariable String ref, Authentication auth, Model m) {
        Booking b = service.byRef(ref).orElseThrow();
        boolean admin = auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !b.getUser().getEmail().equalsIgnoreCase(auth.getName())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
        m.addAttribute("booking", b);
        return "ticket";
    }

    @PostMapping("/{ref}/cancel")
    String cancel(@PathVariable String ref, Authentication a) {
        boolean admin = a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
        service.cancel(ref, a.getName(), admin);
        return "redirect:/bookings";
    }

    @GetMapping("/{ref}/pdf")
    ResponseEntity<ByteArrayResource> pdf(@PathVariable String ref, Authentication auth) throws Exception {
        Booking b = service.byRef(ref).orElseThrow();
        boolean admin = auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !b.getUser().getEmail().equalsIgnoreCase(auth.getName())) {
            return ResponseEntity.status(403).build();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("SKYRESERVE E-TICKET"));
        document.add(new Paragraph("Booking Reference: " + b.getBookingReference()));
        document.add(new Paragraph("Passenger: " + b.getPassengerName()));
        document.add(new Paragraph("Flight: " + b.getSchedule().getFlight().getFlightNumber()));
        document.add(new Paragraph("Route: " + b.getSchedule().getFlight().getSource() + " -> " + b.getSchedule().getFlight().getDestination()));
        document.add(new Paragraph("Travel Date: " + b.getSchedule().getTravelDate()));
        document.add(new Paragraph("Seat: " + b.getSeatNumber()));
        document.add(new Paragraph("Class: " + b.getSeatClass()));
        document.add(new Paragraph(String.format("Amount: INR %.2f", b.getAmount())));
        document.add(new Paragraph("Payment Method: " + b.getPaymentMethod()));
        document.add(new Paragraph("Payment Status: " + b.getPaymentStatus()));
        document.add(new Paragraph("Status: " + b.getStatus()));
        document.close();
        ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SkyReserve-" + b.getBookingReference() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(resource.contentLength())
                .body(resource);
    }

    private List<String> generateSeats(int totalSeats) {
        List<String> seats = new ArrayList<>();
        String[] columns = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < totalSeats; i++) {
            seats.add((i / columns.length + 1) + columns[i % columns.length]);
        }
        return seats;
    }
}
