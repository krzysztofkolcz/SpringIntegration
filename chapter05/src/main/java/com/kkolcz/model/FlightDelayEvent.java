package com.kkolcz.model;

import java.util.Date;

public class FlightDelayEvent {
  private Flight flight;
  private Date estimatedDeparture;

  public FlightDelayEvent( Flight flight,Date estimatedDeparture){
    this.flight = flight;
    this.estimatedDeparture = estimatedDeparture;
  } 

  public void setFlight(Flight flight){
    this.flight = flight;
  }

  public Flight getFlight(){
    return this.flight;
  }

  public void setEstimatedDeparture(Date estimatedDeparture){
    this.estimatedDeparture = estimatedDeparture;
  }

  public Date getEstimatedDeparture(){
    return this.estimatedDeparture;
  }
}
