package com.kkolcz.model;

import java.util.Date;

public class FlightDelayEvent {
  private Flight flight;
  private Date estimatedDeparture;

  public setFlight(Flight flight){
    this.flight = flight;
  }

  public getFlight(){
    return this.flight;
  }

  public setEstimatedDeparture(Date estimatedDeparture){
    this.estimatedDeparture = estimatedDeparture;
  }

  public getEstimatedDeparture(){
    return this.estimatedDeparture;
  }
}
