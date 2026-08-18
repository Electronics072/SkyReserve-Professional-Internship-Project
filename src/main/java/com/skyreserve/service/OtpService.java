package com.skyreserve.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class OtpService {
    private static final String OTP = "paymentOtp";
    private static final String EXPIRES = "paymentOtpExpires";
    private static final String ATTEMPTS = "paymentOtpAttempts";
    private static final String SENT_AT = "paymentOtpSentAt";

    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Creates a short-lived payment verification OTP and tries to email it.
     * If SMTP is not configured, the generated OTP is returned for demo-mode UI.
     */
    public String sendPaymentOtp(String email, HttpSession session) {
        long now = System.currentTimeMillis();
        Object previous = session.getAttribute(SENT_AT);
        if (previous instanceof Long && now - (Long) previous < 30_000L) {
            throw new IllegalStateException("Please wait 30 seconds before requesting another OTP.");
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));
        session.setAttribute(OTP, otp);
        session.setAttribute(EXPIRES, now + 5 * 60_000L);
        session.setAttribute(ATTEMPTS, 0);
        session.setAttribute(SENT_AT, now);
        session.setAttribute("otpVerified", false);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("SkyReserve Payment Verification OTP");
            message.setText("Your SkyReserve payment verification OTP is " + otp
                    + ". It is valid for 5 minutes. Do not share this OTP with anyone.");
            mailSender.send(message);
            return null;
        } catch (Exception ex) {
            // Demo deployments may not have SMTP credentials. The UI can safely
            // expose this code only as a clearly labelled demo fallback.
            return otp;
        }
    }

    public void verifyPaymentOtp(String otp, HttpSession session) {
        String expected = (String) session.getAttribute(OTP);
        Long expires = (Long) session.getAttribute(EXPIRES);
        Integer attempts = (Integer) session.getAttribute(ATTEMPTS);

        if (expected == null || expires == null || System.currentTimeMillis() > expires) {
            clear(session);
            throw new IllegalStateException("OTP expired. Please request a new OTP.");
        }
        if (attempts != null && attempts >= 5) {
            clear(session);
            throw new IllegalStateException("Too many incorrect OTP attempts. Please request a new OTP.");
        }

        if (otp == null || !expected.equals(otp.trim())) {
            session.setAttribute(ATTEMPTS, (attempts == null ? 0 : attempts) + 1);
            throw new IllegalStateException("Invalid OTP. Please try again.");
        }

        session.setAttribute("otpVerified", true);
        session.removeAttribute(OTP);
        session.removeAttribute(EXPIRES);
        session.removeAttribute(ATTEMPTS);
    }

    public boolean isVerified(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("otpVerified"));
    }

    public void clear(HttpSession session) {
        session.removeAttribute(OTP);
        session.removeAttribute(EXPIRES);
        session.removeAttribute(ATTEMPTS);
        session.removeAttribute(SENT_AT);
        session.removeAttribute("otpVerified");
    }
}
