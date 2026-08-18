package com.skyreserve.model;
import jakarta.persistence.*;
@Entity @Table(name="flights") public class Flight {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private String flightNumber;
 @Column(nullable=false) private String airline;
 @Column(nullable=false) private String source;
 @Column(nullable=false) private String destination;
 @Column(nullable=false) private String departureTime;
 @Column(nullable=false) private String arrivalTime;
 private double economyPrice; private double businessPrice; private int totalSeats=30; private boolean active=true;
 public Flight(){} public Flight(String n,String a,String s,String d,String dep,String arr,double e,double b,int seats){flightNumber=n;airline=a;source=s;destination=d;departureTime=dep;arrivalTime=arr;economyPrice=e;businessPrice=b;totalSeats=seats;}
 public Long getId(){return id;} public String getFlightNumber(){return flightNumber;} public void setFlightNumber(String v){flightNumber=v;} public String getAirline(){return airline;} public void setAirline(String v){airline=v;} public String getSource(){return source;} public void setSource(String v){source=v;} public String getDestination(){return destination;} public void setDestination(String v){destination=v;} public String getDepartureTime(){return departureTime;} public void setDepartureTime(String v){departureTime=v;} public String getArrivalTime(){return arrivalTime;} public void setArrivalTime(String v){arrivalTime=v;} public double getEconomyPrice(){return economyPrice;} public void setEconomyPrice(double v){economyPrice=v;} public double getBusinessPrice(){return businessPrice;} public void setBusinessPrice(double v){businessPrice=v;} public int getTotalSeats(){return totalSeats;} public void setTotalSeats(int v){totalSeats=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}