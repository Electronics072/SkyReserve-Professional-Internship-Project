package com.skyreserve.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.razorpay.Order;
import com.skyreserve.model.*;
import com.skyreserve.repository.FlightRepository;
import com.skyreserve.repository.FlightScheduleRepository;
import com.skyreserve.service.BookingService;
import com.skyreserve.service.OtpService;
import com.skyreserve.service.RazorpayService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final FlightRepository flights;
    private final FlightScheduleRepository schedules;
    private final BookingService service;
    private final RazorpayService razorpay;
    private final OtpService otpService;

    public BookingController(
            FlightRepository f,
            FlightScheduleRepository s,
            BookingService b,
            RazorpayService r,
            OtpService o) {

        flights = f;
        schedules = s;
        service = b;
        razorpay = r;
        otpService = o;
    }


    // ============================================================
    // BOOKING FORM
    // ============================================================

    @GetMapping("/new")
    String form(
            @RequestParam Long flightId,
            @RequestParam(required = false) LocalDate date,
            Model m) {

        Flight f = flights.findById(flightId).orElseThrow();

        LocalDate d =
                date == null
                        ? LocalDate.now().plusDays(1)
                        : date;

        FlightSchedule sc =
                schedules
                        .findByFlightIdAndTravelDate(
                                flightId,
                                d
                        )
                        .orElseGet(
                                () -> schedules.save(
                                        new FlightSchedule(f, d)
                                )
                        );

        m.addAttribute("schedule", sc);
        m.addAttribute("flight", f);
        m.addAttribute(
                "seats",
                generateSeats(f.getTotalSeats())
        );
        m.addAttribute(
                "occupiedSeats",
                service.occupiedSeats(sc.getId())
        );

        return "booking";
    }


    // ============================================================
    // CHECKOUT PAGE
    // ============================================================

    @PostMapping("/checkout")
    String checkout(
            @RequestParam Long scheduleId,
            @RequestParam String passengerName,
            @RequestParam String seatNumber,
            @RequestParam String seatClass,
            Model m) {

        if (passengerName == null ||
                passengerName.trim().isEmpty()) {

            m.addAttribute(
                    "error",
                    "Passenger name is required."
            );

            return "error";
        }

        FlightSchedule sc =
                schedules.findById(scheduleId)
                        .orElseThrow();

        m.addAttribute("schedule", sc);
        m.addAttribute(
                "flight",
                sc.getFlight()
        );
        m.addAttribute(
                "passengerName",
                passengerName.trim()
        );
        m.addAttribute(
                "seatNumber",
                seatNumber
        );
        m.addAttribute(
                "seatClass",
                seatClass
        );

        double fare =
                "Business".equalsIgnoreCase(seatClass)
                        ? sc.getFlight().getBusinessPrice()
                        : sc.getFlight().getEconomyPrice();

        double fee =
                Math.round(
                        fare * 0.05 * 100.0
                ) / 100.0;

        m.addAttribute(
                "fare",
                fare
        );

        m.addAttribute(
                "serviceFee",
                fee
        );

        m.addAttribute(
                "total",
                fare + fee
        );

        m.addAttribute(
                "razorpayEnabled",
                !razorpay.getKeyId().isBlank()
        );

        return "payment";
    }


    // ============================================================
    // SEND PAYMENT OTP
    // ============================================================

    @PostMapping("/razorpay/otp/send")
    @ResponseBody
    Object sendPaymentOtp(
            @RequestParam Long scheduleId,
            @RequestParam String passengerName,
            @RequestParam String seatNumber,
            @RequestParam String seatClass,
            Authentication auth,
            HttpSession session) {

        try {

            schedules.findById(scheduleId)
                    .orElseThrow(
                            () -> new IllegalArgumentException(
                                    "Schedule not found"
                            )
                    );

            // Save payment information in session
            session.setAttribute(
                    "rzScheduleId",
                    scheduleId
            );

            session.setAttribute(
                    "rzPassengerName",
                    passengerName.trim()
            );

            session.setAttribute(
                    "rzSeatNumber",
                    seatNumber.trim().toUpperCase()
            );

            session.setAttribute(
                    "rzSeatClass",
                    seatClass
            );

            session.setAttribute(
                    "rzEmail",
                    auth.getName()
            );

            String demoOtp =
                    otpService.sendPaymentOtp(
                            auth.getName(),
                            session
                    );

            String masked =
                    maskEmail(auth.getName());

            Map<String, Object> result =
                    new HashMap<>();

            result.put(
                    "success",
                    true
            );

            result.put(
                    "message",
                    "OTP sent to " + masked
            );

            if (demoOtp != null) {

                result.put(
                        "demoOtp",
                        demoOtp
                );
            }

            return result;

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "success",
                                    false,
                                    "error",
                                    safeMessage(e)
                            )
                    );
        }
    }


    // ============================================================
    // VERIFY PAYMENT OTP
    // ============================================================

    @PostMapping("/razorpay/otp/verify")
    @ResponseBody
    Object verifyPaymentOtp(
            @RequestParam String otp,
            HttpSession session) {

        try {

            otpService.verifyPaymentOtp(
                    otp,
                    session
            );

            return Map.of(
                    "success",
                    true,
                    "message",
                    "OTP verified. You can now continue to Razorpay."
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "success",
                                    false,
                                    "error",
                                    safeMessage(e)
                            )
                    );
        }
    }


    // ============================================================
    // CREATE RAZORPAY ORDER
    // ============================================================

    @PostMapping("/razorpay/order")
    @ResponseBody
    Object createRazorpayOrder(
            Authentication auth,
            HttpSession session) {

        try {

            // ----------------------------------------------------
            // OTP MUST BE VERIFIED
            // ----------------------------------------------------

            if (!otpService.isVerified(session)) {

                throw new IllegalStateException(
                        "Verify the payment OTP first."
                );
            }


            // ----------------------------------------------------
            // READ SESSION VALUES
            // ----------------------------------------------------

            Long scheduleId =
                    getLongFromSession(
                            session,
                            "rzScheduleId"
                    );

            String passenger =
                    getStringFromSession(
                            session,
                            "rzPassengerName"
                    );

            String seat =
                    getStringFromSession(
                            session,
                            "rzSeatNumber"
                    );

            String seatClass =
                    getStringFromSession(
                            session,
                            "rzSeatClass"
                    );

            String email =
                    getStringFromSession(
                            session,
                            "rzEmail"
                    );


            // ----------------------------------------------------
            // VALIDATE SESSION
            // ----------------------------------------------------

            if (scheduleId == null ||
                    passenger == null ||
                    seat == null ||
                    seatClass == null ||
                    email == null) {

                throw new IllegalStateException(
                        "Payment session expired. Please start checkout again."
                );
            }

            if (!email.equalsIgnoreCase(
                    auth.getName()
            )) {

                throw new IllegalStateException(
                        "Payment session does not belong to the current user."
                );
            }


            // ----------------------------------------------------
            // FIND SCHEDULE
            // ----------------------------------------------------

            FlightSchedule sc =
                    schedules.findById(scheduleId)
                            .orElseThrow(
                                    () -> new IllegalArgumentException(
                                            "Schedule not found"
                                    )
                            );


            // ----------------------------------------------------
            // CALCULATE FARE
            // ----------------------------------------------------

            double fare =
                    "Business".equalsIgnoreCase(seatClass)
                            ? sc.getFlight().getBusinessPrice()
                            : sc.getFlight().getEconomyPrice();

            double fee =
                    Math.round(
                            fare * 0.05 * 100.0
                    ) / 100.0;

            double total =
                    fare + fee;


            // ----------------------------------------------------
            // CREATE RAZORPAY ORDER
            // ----------------------------------------------------

            String receipt =
                    "SR-" +
                            System.currentTimeMillis();

            Order order =
                    razorpay.createOrder(
                            total,
                            receipt
                    );


            // ----------------------------------------------------
            // SAFELY READ RAZORPAY ORDER VALUES
            // ----------------------------------------------------

            Object rawOrderId =
                    order.get("id");

            if (rawOrderId == null) {

                throw new IllegalStateException(
                        "Razorpay did not return an order ID."
                );
            }

            String orderId =
                    String.valueOf(
                            rawOrderId
                    );


            Object rawAmount =
                    order.get("amount");

            if (rawAmount == null) {

                throw new IllegalStateException(
                        "Razorpay did not return an order amount."
                );
            }

            long amount;

            if (rawAmount instanceof Number) {

                amount =
                        ((Number) rawAmount)
                                .longValue();

            } else {

                amount =
                        Long.parseLong(
                                String.valueOf(
                                        rawAmount
                                )
                        );
            }


            Object rawCurrency =
                    order.get("currency");

            String currency =
                    rawCurrency == null
                            ? "INR"
                            : String.valueOf(
                            rawCurrency
                    );


            // ----------------------------------------------------
            // SAVE ORDER ID AS STRING
            // ----------------------------------------------------

            session.setAttribute(
                    "rzOrderId",
                    orderId
            );


            // ----------------------------------------------------
            // SEND RESPONSE TO payment.html
            // ----------------------------------------------------

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "key",
                    razorpay.getKeyId()
            );

            response.put(
                    "orderId",
                    orderId
            );

            response.put(
                    "amount",
                    amount
            );

            response.put(
                    "currency",
                    currency
            );

            response.put(
                    "name",
                    "SkyReserve"
            );

            response.put(
                    "description",
                    "Flight reservation payment"
            );

            return response;

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "success",
                                    false,
                                    "error",
                                    safeMessage(e)
                            )
                    );
        }
    }


    // ============================================================
    // VERIFY RAZORPAY PAYMENT
    // ============================================================

    @PostMapping("/razorpay/verify")
    @ResponseBody
    Object verifyRazorpayPayment(
            @RequestParam String razorpay_order_id,
            @RequestParam String razorpay_payment_id,
            @RequestParam String razorpay_signature,
            Authentication auth,
            HttpSession session) {

        try {

            // ----------------------------------------------------
            // OTP MUST BE VERIFIED
            // ----------------------------------------------------

            if (!otpService.isVerified(session)) {

                throw new IllegalStateException(
                        "Payment OTP verification is required."
                );
            }


            // ----------------------------------------------------
            // READ STORED ORDER ID SAFELY
            // ----------------------------------------------------

            String serverOrderId =
                    getStringFromSession(
                            session,
                            "rzOrderId"
                    );

            if (serverOrderId == null ||
                    serverOrderId.isBlank()) {

                throw new IllegalStateException(
                        "Payment order session expired."
                );
            }


            // ----------------------------------------------------
            // CHECK ORDER ID
            // ----------------------------------------------------

            if (!serverOrderId.equals(
                    razorpay_order_id
            )) {

                throw new IllegalStateException(
                        "Invalid payment order."
                );
            }


            // ----------------------------------------------------
            // VERIFY RAZORPAY SIGNATURE
            // ----------------------------------------------------

            boolean verified =
                    razorpay.verify(
                            serverOrderId,
                            razorpay_payment_id,
                            razorpay_signature
                    );

            if (!verified) {

                throw new IllegalStateException(
                        "Payment signature verification failed."
                );
            }


            // ----------------------------------------------------
            // READ BOOKING SESSION DATA
            // ----------------------------------------------------

            String email =
                    getStringFromSession(
                            session,
                            "rzEmail"
                    );

            Long scheduleId =
                    getLongFromSession(
                            session,
                            "rzScheduleId"
                    );

            String passenger =
                    getStringFromSession(
                            session,
                            "rzPassengerName"
                    );

            String seat =
                    getStringFromSession(
                            session,
                            "rzSeatNumber"
                    );

            String seatClass =
                    getStringFromSession(
                            session,
                            "rzSeatClass"
                    );


            // ----------------------------------------------------
            // VALIDATE BOOKING SESSION
            // ----------------------------------------------------

            if (email == null ||
                    scheduleId == null ||
                    passenger == null ||
                    seat == null ||
                    seatClass == null) {

                throw new IllegalStateException(
                        "Payment session expired. Please start checkout again."
                );
            }

            if (!email.equalsIgnoreCase(
                    auth.getName()
            )) {

                throw new IllegalStateException(
                        "Payment session does not belong to the current user."
                );
            }


            // ----------------------------------------------------
            // CREATE BOOKING
            // ----------------------------------------------------

            Booking b =
                    service.create(
                            scheduleId,
                            email,
                            passenger,
                            seat,
                            seatClass,
                            "Razorpay TEST"
                    );


            // ----------------------------------------------------
            // CLEAR PAYMENT SESSION
            // ----------------------------------------------------

            otpService.clear(session);

            session.removeAttribute(
                    "rzOrderId"
            );

            session.removeAttribute(
                    "rzEmail"
            );

            session.removeAttribute(
                    "rzScheduleId"
            );

            session.removeAttribute(
                    "rzPassengerName"
            );

            session.removeAttribute(
                    "rzSeatNumber"
            );

            session.removeAttribute(
                    "rzSeatClass"
            );


            // ----------------------------------------------------
            // RETURN SUCCESS
            // ----------------------------------------------------

            return Map.of(
                    "success",
                    true,
                    "bookingReference",
                    b.getBookingReference(),
                    "paymentId",
                    razorpay_payment_id
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "success",
                                    false,
                                    "error",
                                    safeMessage(e)
                            )
                    );
        }
    }


    // ============================================================
    // NORMAL BOOKING CREATION
    // ============================================================

    @PostMapping("/create")
    String create(
            @RequestParam Long scheduleId,
            @RequestParam String passengerName,
            @RequestParam String seatNumber,
            @RequestParam String seatClass,
            @RequestParam String paymentMethod,
            Authentication auth,
            Model m) {

        try {

            Booking b =
                    service.create(
                            scheduleId,
                            auth.getName(),
                            passengerName,
                            seatNumber,
                            seatClass,
                            paymentMethod
                    );

            return "redirect:/bookings/"
                    + b.getBookingReference();

        } catch (Exception e) {

            m.addAttribute(
                    "error",
                    safeMessage(e)
            );

            return "error";
        }
    }


    // ============================================================
    // MY BOOKINGS
    // ============================================================

    @GetMapping
    String mine(
            Authentication a,
            Model m) {

        m.addAttribute(
                "bookings",
                service.mine(a.getName())
        );

        return "bookings";
    }


    // ============================================================
    // VIEW BOOKING
    // ============================================================

    @GetMapping("/{ref}")
    String view(
            @PathVariable String ref,
            Authentication auth,
            Model m) {

        Booking b =
                service.byRef(ref)
                        .orElseThrow();

        boolean admin =
                auth.getAuthorities()
                        .stream()
                        .anyMatch(
                                x -> x.getAuthority()
                                        .equals("ROLE_ADMIN")
                        );

        if (!admin &&
                !b.getUser()
                        .getEmail()
                        .equalsIgnoreCase(
                                auth.getName()
                        )) {

            throw new org.springframework.web.server
                    .ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN
            );
        }

        m.addAttribute(
                "booking",
                b
        );

        return "ticket";
    }


    // ============================================================
    // CANCEL BOOKING
    // ============================================================

    @PostMapping("/{ref}/cancel")
    String cancel(
            @PathVariable String ref,
            Authentication a) {

        boolean admin =
                a.getAuthorities()
                        .stream()
                        .anyMatch(
                                x -> x.getAuthority()
                                        .equals("ROLE_ADMIN")
                        );

        service.cancel(
                ref,
                a.getName(),
                admin
        );

        return "redirect:/bookings";
    }


    // ============================================================
    // PDF TICKET
    // ============================================================

    @GetMapping("/{ref}/pdf")
    ResponseEntity<ByteArrayResource> pdf(
            @PathVariable String ref,
            Authentication auth)
            throws Exception {

        Booking b =
                service.byRef(ref)
                        .orElseThrow();

        boolean admin =
                auth.getAuthorities()
                        .stream()
                        .anyMatch(
                                x -> x.getAuthority()
                                        .equals("ROLE_ADMIN")
                        );

        if (!admin &&
                !b.getUser()
                        .getEmail()
                        .equalsIgnoreCase(
                                auth.getName()
                        )) {

            return ResponseEntity
                    .status(403)
                    .build();
        }

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        Document document =
                new Document();

        PdfWriter.getInstance(
                document,
                out
        );

        document.open();

        document.add(
                new Paragraph(
                        "SKYRESERVE E-TICKET"
                )
        );

        document.add(
                new Paragraph(
                        "Booking Reference: "
                                + b.getBookingReference()
                )
        );

        document.add(
                new Paragraph(
                        "Passenger: "
                                + b.getPassengerName()
                )
        );

        document.add(
                new Paragraph(
                        "Flight: "
                                + b.getSchedule()
                                .getFlight()
                                .getFlightNumber()
                )
        );

        document.add(
                new Paragraph(
                        "Route: "
                                + b.getSchedule()
                                .getFlight()
                                .getSource()
                                + " -> "
                                + b.getSchedule()
                                .getFlight()
                                .getDestination()
                )
        );

        document.add(
                new Paragraph(
                        "Travel Date: "
                                + b.getSchedule()
                                .getTravelDate()
                )
        );

        document.add(
                new Paragraph(
                        "Seat: "
                                + b.getSeatNumber()
                )
        );

        document.add(
                new Paragraph(
                        "Class: "
                                + b.getSeatClass()
                )
        );

        document.add(
                new Paragraph(
                        String.format(
                                "Amount: INR %.2f",
                                b.getAmount()
                        )
                )
        );

        document.add(
                new Paragraph(
                        "Payment Method: "
                                + b.getPaymentMethod()
                )
        );

        document.add(
                new Paragraph(
                        "Payment Status: "
                                + b.getPaymentStatus()
                )
        );

        document.add(
                new Paragraph(
                        "Status: "
                                + b.getStatus()
                )
        );

        document.close();

        ByteArrayResource resource =
                new ByteArrayResource(
                        out.toByteArray()
                );

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=SkyReserve-"
                                + b.getBookingReference()
                                + ".pdf"
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .contentLength(
                        resource.contentLength()
                )
                .body(resource);
    }


    // ============================================================
    // GENERATE SEATS
    // ============================================================

    private List<String> generateSeats(
            int totalSeats) {

        List<String> seats =
                new ArrayList<>();

        String[] columns = {
                "A",
                "B",
                "C",
                "D",
                "E",
                "F"
        };

        for (int i = 0;
             i < totalSeats;
             i++) {

            seats.add(
                    (i / columns.length + 1)
                            + columns[
                            i % columns.length
                            ]
            );
        }

        return seats;
    }


    // ============================================================
    // SAFE SESSION STRING
    // ============================================================

    private String getStringFromSession(
            HttpSession session,
            String attributeName) {

        Object value =
                session.getAttribute(
                        attributeName
                );

        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }


    // ============================================================
    // SAFE SESSION LONG
    // ============================================================

    private Long getLongFromSession(
            HttpSession session,
            String attributeName) {

        Object value =
                session.getAttribute(
                        attributeName
                );

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        try {

            return Long.parseLong(
                    String.valueOf(value)
            );

        } catch (NumberFormatException e) {

            return null;
        }
    }


    // ============================================================
    // MASK EMAIL
    // ============================================================

    private String maskEmail(
            String email) {

        int at =
                email.indexOf('@');

        if (at <= 1) {

            return "your registered email";
        }

        return email.charAt(0)
                + "***"
                + email.substring(at - 1);
    }


    // ============================================================
    // SAFE ERROR MESSAGE
    // ============================================================

    private String safeMessage(
            Exception e) {

        if (e.getMessage() == null ||
                e.getMessage().isBlank()) {

            return e.getClass()
                    .getSimpleName();
        }

        return e.getMessage();
    }
}