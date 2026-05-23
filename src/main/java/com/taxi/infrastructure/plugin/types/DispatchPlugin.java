package com.taxi.infrastructure.plugin.types;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.drivertrip.model.DriverTrip;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.infrastructure.plugin.core.Plugin;

import java.time.LocalDate;
import java.util.List;

/**
 * Plugin interface for dispatch system integrations (TaxiCaller, iCabbi, TripMaster, etc.).
 * Supports importing driver shifts, trips, and account charges from external dispatch systems.
 */
public interface DispatchPlugin extends Plugin {

    /**
     * Import driver shifts (logon/logoff records) for a date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of imported driver shifts
     */
    List<DriverShift> importDriverShifts(LocalDate startDate, LocalDate endDate);

    /**
     * Import individual trips for a date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of imported trips
     */
    List<DriverTrip> importTrips(LocalDate startDate, LocalDate endDate);

    /**
     * Import account charges (corporate/charge account trips) for a date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of imported account charges
     */
    List<AccountCharge> importAccountCharges(LocalDate startDate, LocalDate endDate);
}
