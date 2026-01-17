package com.taxi.domain.account.service;

import com.taxi.domain.account.dto.TaxiCallerImportResult;
import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.model.AccountCustomer;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.account.repository.AccountCustomerRepository;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class TaxiCallerAccountChargeImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaxiCallerAccountChargeImportService.class);
    
    // Date/Time formatters for TaxiCaller format
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // @Autowired
    // private TaxiCallerService taxiCallerService;
    
    @Autowired
    private AccountChargeRepository accountChargeRepository;
    
    @Autowired
    private AccountCustomerRepository accountCustomerRepository;
    
    @Autowired
    private CabRepository cabRepository;
    
    @Autowired
    private DriverRepository driverRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Import account job reports from TaxiCaller and save to account_charge table
     */
    @Transactional
    public TaxiCallerImportResult importAccountJobReports(JSONArray taxiCallerTrips) {
        TaxiCallerImportResult result = new TaxiCallerImportResult();
        
        try {
            if (taxiCallerTrips == null || taxiCallerTrips.length() == 0) {
                logger.warn("No data returned from TaxiCaller API");
                return result;
            }
            
            result.setTotalRecords(taxiCallerTrips.length());
            logger.info("Processing {} account job reports from TaxiCaller", taxiCallerTrips.length());
            
            // Process each trip
            for (int i = 0; i < taxiCallerTrips.length(); i++) {
                try {
                    JSONObject tripData = taxiCallerTrips.getJSONObject(i);
                    String jobId = tripData.optString("job_id", null);
                    
                    // Skip if no job_id
                    if (jobId == null || jobId.trim().isEmpty()) {
                        result.incrementError("Missing job_id at index " + i);
                        logger.warn("Skipping record at index {} - missing job_id", i);
                        continue;
                    }
                    
                    // Transform TaxiCaller data to AccountCharge
                    AccountCharge charge = transformTaxiCallerToAccountCharge(tripData);
                    
                    // Check if record already exists by unique constraint fields
                    java.util.Optional<AccountCharge> existingCharge = accountChargeRepository.findByUniqueConstraint(
                            charge.getAccountId(),
                            charge.getCab() != null ? charge.getCab().getId() : null,
                            charge.getDriver() != null ? charge.getDriver().getId() : null,
                            charge.getTripDate(),
                            charge.getStartTime(),
                            charge.getJobCode()
                    );
                    
                    if (existingCharge.isPresent()) {
                        // Update existing record instead of creating duplicate
                        // AccountCharge existing = existingCharge.get();
                        // updateExistingCharge(existing, charge);
                        // accountChargeRepository.save(existing);
                        // result.incrementDuplicate(jobId);
                        logger.debug("Duplicate ignore record job_id: {}", jobId);
                    } else {
                        // Save new charge
                        accountChargeRepository.save(charge);
                        result.incrementSuccess();
                    }
                    
                    if ((i + 1) % 50 == 0) {
                        logger.info("Processed {} / {} records", i + 1, taxiCallerTrips.length());
                    }
                    
                } catch (Exception e) {
                    String jobId = "unknown";
                    try {
                        jobId = taxiCallerTrips.getJSONObject(i).optString("job_id", "unknown");
                    } catch (Exception ex) {
                        // ignore
                    }
                    
                    String errorMsg = String.format("Error processing record at index %d (job_id: %s): %s", i, jobId, e.getMessage());
                    result.incrementError(errorMsg);
                    logger.error(errorMsg, e);
                }
            }
            
            logger.info("Import completed: {}", result.toString());
            
        } catch (Exception e) {
            logger.error("Fatal error during import", e);
            result.incrementError("Fatal error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Update existing charge with new data (preserves ID and certain fields)
     */
    private void updateExistingCharge(AccountCharge existing, AccountCharge newData) {
        // Update fields that may have changed
        existing.setSubAccount(newData.getSubAccount());
        existing.setAccountCustomer(newData.getAccountCustomer());
        existing.setEndTime(newData.getEndTime());
        existing.setPickupAddress(newData.getPickupAddress());
        existing.setDropoffAddress(newData.getDropoffAddress());
        existing.setPassengerName(newData.getPassengerName());
        existing.setCab(newData.getCab());
        existing.setDriver(newData.getDriver());
        existing.setFareAmount(newData.getFareAmount());
        existing.setTipAmount(newData.getTipAmount());
        existing.setNotes(newData.getNotes());
        // Don't update: id, accountId, tripDate, startTime, jobCode (these are part of unique key)
        // Don't update: paid, paidDate, invoiceNumber, invoiceId (billing status should be preserved)
    }
    
    /**
     * Transform TaxiCaller JSON to AccountCharge entity
     * 
     * TaxiCaller format:
     * {
     *   "job_id": "297227182",
     *   "account_num": "9777 48787772",
     *   "date": "02/12/2025",
     *   "start": "02/12/2025 06:42",
     *   "end": "02/12/2025 06:51",
     *   "driver_id": "224855",
     *   "driver": "SINGH SAHDRA, SARBJIT",
     *   "vehicle_num": "150",
     *   "pick-up": "2228 West 18th Avenue...",
     *   "drop_off": "2211 West 4th Avenue...",
     *   "passenger": "REA GALE WCB",
     *   "tariff": "8.45",
     *   "payable": "8.45",
     *   "cost_code": "48787772"
     * }
     */
    private AccountCharge transformTaxiCallerToAccountCharge(JSONObject tripData) {
        AccountCharge charge = new AccountCharge();
        
        // Parse job_id
        String jobId = tripData.optString("job_id");
        charge.setJobCode(jobId);
        
        // Parse account_num (e.g., "9777 48787772")
        String accountNum = tripData.optString("account_num");
        AccountInfo accountInfo = parseAccountNumber(accountNum);
        charge.setAccountId(accountInfo.accountId);        // "9777"
        charge.setSubAccount(accountInfo.subAccount);      // "48787772" or null
        
        // Find or create AccountCustomer
        AccountCustomer customer = findOrCreateCustomer(accountNum, tripData.optString("passenger"));
        charge.setAccountCustomer(customer);
        
        // Parse dates and times
        String dateStr = tripData.optString("date"); // "02/12/2025"
        String startStr = tripData.optString("start"); // "02/12/2025 06:42"
        String endStr = tripData.optString("end"); // "02/12/2025 06:51"
        
        charge.setTripDate(parseDate(dateStr));
        charge.setStartTime(parseTime(startStr));
        charge.setEndTime(parseTime(endStr));
        
        // Parse addresses
        charge.setPickupAddress(tripData.optString("pick-up"));
        charge.setDropoffAddress(tripData.optString("drop_off"));
        
        // Parse passenger
        charge.setPassengerName(tripData.optString("passenger"));
        
        // Find Cab by vehicle_num
        String vehicleNum = tripData.optString("vehicle_num"); // "150"
        Cab cab = findCabByVehicleNumber(vehicleNum);
        charge.setCab(cab);
        
        // Find Driver by TaxiCaller driver_id
        String taxiCallerDriverId = tripData.optString("driver_id"); // "224855"
        Driver driver = findDriverByExternalId(taxiCallerDriverId, tripData.optString("driver"));
        charge.setDriver(driver);
        
        // Parse financial amounts
        charge.setFareAmount(parseBigDecimal(tripData.optString("payable")));
        charge.setTipAmount(BigDecimal.ZERO); // TaxiCaller doesn't provide tip info
        
        // Build notes from additional fields
        StringBuilder notes = new StringBuilder();
        if (!tripData.optString("cost_code", "").isEmpty()) {
            notes.append("Cost Code: ").append(tripData.optString("cost_code"));
        }
        if (!tripData.optString("reference", "").isEmpty()) {
            if (notes.length() > 0) notes.append("; ");
            notes.append("Ref: ").append(tripData.optString("reference"));
        }
        if (!tripData.optString("project", "").isEmpty()) {
            if (notes.length() > 0) notes.append("; ");
            notes.append("Project: ").append(tripData.optString("project"));
        }
        charge.setNotes(notes.length() > 0 ? notes.toString() : null);
        
        // Set as unpaid by default
        charge.setPaid(false);
        
        return charge;
    }
    
    /**
     * Inner class to hold parsed account information
     */
    private static class AccountInfo {
        String accountId;
        String subAccount;
        
        AccountInfo(String accountId, String subAccount) {
            this.accountId = accountId;
            this.subAccount = subAccount;
        }
    }
    
    /**
     * Parse account number into account_id and sub_account
     * 
     * Examples:
     *   "9777 48787772" → accountId: "9777", subAccount: "48787772"
     *   "1234 56789"    → accountId: "1234", subAccount: "56789"
     *   "5555"          → accountId: "5555", subAccount: null
     *   ""              → accountId: "UNKNOWN", subAccount: null
     */
    private AccountInfo parseAccountNumber(String accountNum) {
        if (accountNum == null || accountNum.trim().isEmpty()) {
            return new AccountInfo("UNKNOWN", null);
        }
        
        accountNum = accountNum.trim();
        
        // If contains space, split into account_id and sub_account
        if (accountNum.contains(" ")) {
            String[] parts = accountNum.split("\\s+");
            String accountId = parts[0];
            String subAccount = parts.length > 1 ? parts[1] : null;
            return new AccountInfo(accountId, subAccount);
        }
        
        // No space, just account_id
        return new AccountInfo(accountNum, null);
    }
    
    /**
     * Extract account_id from account_num (for backward compatibility)
     * 
     * @deprecated Use parseAccountNumber() instead
     */
    @Deprecated
    private String extractAccountId(String accountNum) {
        return parseAccountNumber(accountNum).accountId;
    }
    
    /**
     * Find or create AccountCustomer
     * Store full account_num (e.g., "9777 48787772") in customer record
     * But use parent account_id (e.g., "9777") for grouping
     */
    private AccountCustomer findOrCreateCustomer(String accountNum, String passengerName) {
        String accountId = extractAccountId(accountNum);
        
        // Try to find existing customer by account_num
        AccountCustomer customer = accountCustomerRepository.findByAccountId(accountId)
                .stream()
                .findFirst()
                .orElse(null);
        
        if (customer == null) {
            // Create new customer
            customer = new AccountCustomer();
            customer.setAccountId(accountId);
            customer.setCompanyName(accountNum); // Store full account_num as company name initially
            customer.setActive(true);
            customer = accountCustomerRepository.save(customer);
            logger.info("Created new AccountCustomer for account_id: {} (full: {})", accountId, accountNum);
        }
        
        return customer;
    }
    
    /**
     * Find Cab by vehicle number (cab_number in database)
     */
    private Cab findCabByVehicleNumber(String vehicleNum) {
        if (vehicleNum == null || vehicleNum.trim().isEmpty()) {
            logger.warn("Missing vehicle_num, cannot find cab");
            return null;
        }
        
        return cabRepository.findByCabNumber(vehicleNum.trim())
                .orElseThrow(() -> new RuntimeException("Cab not found with vehicle number: " + vehicleNum));
    }
    
    /**
     * Find Driver by TaxiCaller external ID
     * You may need to add externalId field to Driver entity to store TaxiCaller driver_id
     */
    private Driver findDriverByExternalId(String taxiCallerDriverId, String driverName) {
        if (taxiCallerDriverId == null || taxiCallerDriverId.trim().isEmpty()) {
            logger.warn("Missing driver_id for driver: {}", driverName);
            return null;
        }
        
        // Try to find by external ID (if you have this field)
       // Option 1: If you have externalId field
        return driverRepository.findByDriverNumber(taxiCallerDriverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with external ID: " + taxiCallerDriverId));
        
        // Option 2: If you need to find by name (less reliable)
        // Parse "SINGH SAHDRA, SARBJIT" into lastName="SINGH SAHDRA", firstName="SARBJIT"
        // if (driverName != null && driverName.contains(",")) {
        //     String[] parts = driverName.split(",");
        //     String lastName = parts[0].trim();
        //     String firstName = parts.length > 1 ? parts[1].trim() : "";
            
        //     return driverRepository.findByFirstNameAndLastName(firstName, lastName)
        //             .orElseThrow(() -> new RuntimeException("Driver not found: " + driverName));
        // }
        
        // throw new RuntimeException("Cannot find driver with ID: " + taxiCallerDriverId + ", Name: " + driverName);
    }
    
    /**
     * Parse date string from TaxiCaller format "02/12/2025" to LocalDate
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse date: {}", dateStr, e);
            return null;
        }
    }
    
    /**
     * Parse time string from TaxiCaller format "02/12/2025 06:42" to LocalTime
     */
    private LocalTime parseTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr.trim(), DATETIME_FORMATTER);
            return dateTime.toLocalTime();
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse time from: {}", dateTimeStr, e);
            return null;
        }
    }
    
    /**
     * Parse BigDecimal from string, handling whitespace and invalid values
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Remove any whitespace and currency symbols
            String cleaned = value.trim().replaceAll("[^0-9.-]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse decimal value: {}", value);
            return BigDecimal.ZERO;
        }
    }
}
