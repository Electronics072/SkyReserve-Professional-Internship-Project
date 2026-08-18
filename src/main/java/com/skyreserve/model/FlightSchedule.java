package com.skyreserve.model;
import jakarta.persistence.*; import java.time.LocalDate;
@Entity @Table(name="flight_schedules", uniqueConstraints=@UniqueConstraint(columnNames={"flight_id","travelDate"})) public class FlightSchedule {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) private Flight flight;
 @Column(nullable=false) private LocalDate travelDate;
 private String status="SCHEDULED";
 public FlightSchedule(){} public FlightSchedule(Flight f,LocalDate d){flight=f;travelDate=d;}
 public Long getId(){return id;} public Flight getFlight(){return flight;} public void setFlight(Flight v){flight=v;} public LocalDate getTravelDate(){return travelDate;} public void setTravelDate(LocalDate v){travelDate=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}