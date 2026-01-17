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
    private String shift; // "DAY" or "NIGHT"
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
    
    /**
     * Determine shift based on hour.
     * DAY: 4am-4pm (hours 4-15)
     * NIGHT: 4pm-4am (hours 16-23, 0-3)
     */
    public static String getShiftForHour(int hour) {
        if (hour >= 4 && hour < 16) {
            return "DAY";
        } else {
            return "NIGHT";
        }
    }
}
