package com.kkolcz.model;

public interface FlightStatusService {
  FlightStatus updateStatus(FlightDelayEvent flightDelayEvent);
}
