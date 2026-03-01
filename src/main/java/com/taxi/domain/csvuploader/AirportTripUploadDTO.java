package com.taxi.domain.csvuploader;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirportTripUploadDTO {
    
    private String vehicleName;
    private String cabNumber;
    private String shift = "BOTH"; // All trips in 24-hour period
    private Integer year;
    private Integer month;
    private Integer day;
    private LocalDate tripDate;
    private Map<Integer, Integer> hourlyTrips = new HashMap<>();
    private Integer grandTotal;
    private boolean valid = true;
    private String validationMessage;
    private boolean cabLookupSuccess;
    private String cabLookupMessage;
    private int rowNumber;
    
    public Integer getTripsForHour(int hour) {
        return hourlyTrips.getOrDefault(hour, 0);
    }
    
    public void setTripsForHour(int hour, Integer count) {
        hourlyTrips.put(hour, count != null ? count : 0);
    }
    
    public void calculateGrandTotal() {
        this.grandTotal = hourlyTrips.values().stream()
            .filter(v -> v != null)
            .mapToInt(Integer::intValue)
            .sum();
    }
}
