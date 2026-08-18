package com.skyreserve.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="bookings", uniqueConstraints=@UniqueConstraint(columnNames={"schedule_id","seatNumber","status"})) public class Booking {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private String bookingReference;
 @ManyToOne(optional=false) private User user;
 @ManyToOne(optional=false) private FlightSchedule schedule;
 @Column(nullable=false) private String passengerName;
 @Column(nullable=false) private String seatNumber;
 @Column(nullable=false) private String seatClass;
 private double amount;
 @Column(nullable=false) private String paymentMethod="Demo UPI";
 @Column(nullable=false) private String paymentStatus="PENDING";
 @Enumerated(EnumType.STRING) private BookingStatus status=BookingStatus.CONFIRMED;
 private LocalDateTime createdAt=LocalDateTime.now();
 public Long getId(){return id;} public String getBookingReference(){return bookingReference;} public void setBookingReference(String v){bookingReference=v;} public User getUser(){return user;} public void setUser(User v){user=v;} public FlightSchedule getSchedule(){return schedule;} public void setSchedule(FlightSchedule v){schedule=v;} public String getPassengerName(){return passengerName;} public void setPassengerName(String v){passengerName=v;} public String getSeatNumber(){return seatNumber;} public void setSeatNumber(String v){seatNumber=v;} public String getSeatClass(){return seatClass;} public void setSeatClass(String v){seatClass=v;} public double getAmount(){return amount;} public void setAmount(double v){amount=v;} public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;} public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;} public BookingStatus getStatus(){return status;} public void setStatus(BookingStatus v){status=v;} public LocalDateTime getCreatedAt(){return createdAt;}
}