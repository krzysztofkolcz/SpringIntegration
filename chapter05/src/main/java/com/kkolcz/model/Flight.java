package com.kkolcz.model;

import java.util.Date;
import java.util.Map;
public class Flight {
  private String number;
  private Date scheduledDeparture;
  private String origin;
  private String destination;
  private Equipment aircraft;
  private Crew crew;
  private Map<Seat, Passenger> passengers;

  public void setNumber(String number){
    this.number = number;
  }
  public String getNumber(){
    return this.number;
  }

  public void setScheduledDeparture(Date scheduledDeparture){
    this.scheduledDeparture = scheduledDeparture;
  }

  public Date getScheduledDeparture(){
    return this.scheduledDeparture;
  }
  
  public void setOrigin(String origin){
    this.origin = origin;
  }
  public String getOrigin(){
    return this.origin;
  }

  public void setDestination(String destination){
    this.destination = destination;
  }
  public String getDestination(){
    return this.destination;
  }
  
  public void setAircraft(Equipment aircraft){
    this.aircraft = aircraft;
  }
  public Equipment getAircraft(){
    return this.aircraft;
  }

  public void setCrew(Crew crew){
    this.crew = crew;
  }
  public Crew getCrew(){
    return this.crew;
  }

  public void setPassangers{
    this.passengers = passengers;
  }

  public Map<Seat,Passenger> getPassengers(){
    return this.passengers;
  }
}
