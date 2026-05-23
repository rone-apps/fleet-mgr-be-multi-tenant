package com.taxi.domain.taxicaller;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.service.TaxiCallerAccountChargeImportService;
import com.taxi.domain.drivertrip.model.DriverTrip;
import com.taxi.domain.drivertrip.service.TaxiCallerDriverTripImportService;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.service.TaxiCallerDriverShiftImportService;
import com.taxi.domain.taxicaller.service.TaxiCallerService;
import com.taxi.infrastructure.multitenancy.TenantContext;
import com.taxi.infrastructure.plugin.config.PluginConfig;
import com.taxi.infrastructure.plugin.config.PluginConfigService;
import com.taxi.infrastructure.plugin.core.*;
import com.taxi.infrastructure.plugin.types.DispatchPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * TaxiCaller dispatch system plugin implementation.
 * Wraps existing TaxiCallerService and import services to conform to the plugin architecture.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaxiCallerPlugin implements DispatchPlugin {

    private final TaxiCallerService taxiCallerService;
    private final TaxiCallerDriverShiftImportService driverShiftImportService;
    private final TaxiCallerDriverTripImportService driverTripImportService;
    private final TaxiCallerAccountChargeImportService accountChargeImportService;
    private final PluginConfigService pluginConfigService;

    @Override
    public PluginMetadata getMetadata() {
        return PluginMetadata.builder()
                .pluginId("taxicaller")
                .displayName("TaxiCaller Integration")
                .version("1.0.0")
                .type(PluginType.DISPATCH)
                .capabilities(Set.of("DRIVER_SHIFTS", "TRIPS", "ACCOUNT_CHARGES"))
                .configFields(Arrays.asList(
                        new ConfigField("apiKey", "API Key", FieldType.SECRET, true, null, null),
                        new ConfigField("companyId", "Company ID", FieldType.INTEGER, true, null, null),
                        new ConfigField("baseUrl", "Base URL", FieldType.URL, false,
                                "https://app.taxicaller.net", null)
                ))
                .description("Import driver shifts, trips, and account charges from TaxiCaller dispatch system")
                .vendor("TaxiCaller")
                .build();
    }

    @Override
    public boolean isEnabled(String tenantId) {
        return pluginConfigService.isPluginEnabled(tenantId, "taxicaller");
    }

    @Override
    public List<DriverShift> importDriverShifts(LocalDate startDate, LocalDate endDate) {
        log.info("Importing TaxiCaller driver shifts from {} to {}", startDate, endDate);

        try {
            // Fetch data from TaxiCaller API
            JSONArray rows = taxiCallerService.generateDriverLogOnOffReports(startDate, endDate);

            // Import using existing service (saves to database as side effect)
            var result = driverShiftImportService.importDriverShifts(rows);

            log.info("Successfully imported {} driver shifts (success: {}, duplicates: {}, failed: {})",
                    result.getTotalRecords(), result.getSuccessCount(),
                    result.getDuplicateCount(), result.getFailedCount());

            // Return empty list - entities are saved to DB as side effect
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to import driver shifts from TaxiCaller", e);
            throw new PluginException("Failed to import driver shifts", e);
        }
    }

    @Override
    public List<DriverTrip> importTrips(LocalDate startDate, LocalDate endDate) {
        log.info("Importing TaxiCaller driver trips from {} to {}", startDate, endDate);

        try {
            // Fetch data from TaxiCaller API
            JSONArray rows = taxiCallerService.generateDriverJobReports(startDate, endDate);

            // Import using existing service (saves to database as side effect)
            var result = driverTripImportService.importDriverJobReports(rows);

            log.info("Successfully imported {} driver trips (success: {}, duplicates: {}, failed: {})",
                    result.getTotalRecords(), result.getSuccessCount(),
                    result.getDuplicateCount(), result.getErrorCount());

            // Return empty list - entities are saved to DB as side effect
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to import driver trips from TaxiCaller", e);
            throw new PluginException("Failed to import driver trips", e);
        }
    }

    @Override
    public List<AccountCharge> importAccountCharges(LocalDate startDate, LocalDate endDate) {
        log.info("Importing TaxiCaller account charges from {} to {}", startDate, endDate);

        try {
            // Fetch data from TaxiCaller API
            JSONArray rows = taxiCallerService.generateAccountJobReports(startDate, endDate);

            // Import using existing service (saves to database as side effect)
            var result = accountChargeImportService.importAccountJobReports(rows);

            log.info("Successfully imported {} account charges (success: {}, duplicates: {}, failed: {})",
                    result.getTotalRecords(), result.getSuccessCount(),
                    result.getDuplicateCount(), result.getErrorCount());

            // Return empty list - entities are saved to DB as side effect
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to import account charges from TaxiCaller", e);
            throw new PluginException("Failed to import account charges", e);
        }
    }

    @Override
    public PluginExecutionResult execute(PluginContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        String tenantId = context.getTenantId() != null ? context.getTenantId() : TenantContext.getCurrentTenant();

        log.info("Executing TaxiCaller plugin for tenant: {}", tenantId);

        PluginExecutionResult.PluginExecutionResultBuilder resultBuilder = PluginExecutionResult.builder()
                .pluginId("taxicaller")
                .startTime(startTime);

        int totalRecords = 0;
        int totalSuccess = 0;
        int totalFailed = 0;
        List<String> warnings = new ArrayList<>();

        try {
            // Get date range from context (default to yesterday)
            LocalDate startDate = context.getParameter("startDate", LocalDate.class);
            LocalDate endDate = context.getParameter("endDate", LocalDate.class);

            if (startDate == null) {
                startDate = LocalDate.now().minusDays(1);
                warnings.add("No startDate provided, defaulting to yesterday: " + startDate);
            }

            if (endDate == null) {
                endDate = startDate;
                warnings.add("No endDate provided, defaulting to startDate: " + endDate);
            }

            log.info("Importing TaxiCaller data from {} to {}", startDate, endDate);

            // Import driver shifts
            try {
                JSONArray shiftsData = taxiCallerService.generateDriverLogOnOffReports(startDate, endDate);
                var shiftsResult = driverShiftImportService.importDriverShifts(shiftsData);
                totalRecords += shiftsResult.getTotalRecords();
                totalSuccess += shiftsResult.getSuccessCount();
                totalFailed += shiftsResult.getFailedCount();
                log.info("Imported {} driver shifts (success: {}, failed: {})",
                        shiftsResult.getTotalRecords(), shiftsResult.getSuccessCount(), shiftsResult.getFailedCount());
            } catch (Exception e) {
                log.error("Failed to import driver shifts", e);
                warnings.add("Driver shifts import failed: " + e.getMessage());
                totalFailed++;
            }

            // Import driver trips
            try {
                JSONArray tripsData = taxiCallerService.generateDriverJobReports(startDate, endDate);
                var tripsResult = driverTripImportService.importDriverJobReports(tripsData);
                totalRecords += tripsResult.getTotalRecords();
                totalSuccess += tripsResult.getSuccessCount();
                totalFailed += tripsResult.getErrorCount();
                log.info("Imported {} driver trips (success: {}, failed: {})",
                        tripsResult.getTotalRecords(), tripsResult.getSuccessCount(), tripsResult.getErrorCount());
            } catch (Exception e) {
                log.error("Failed to import driver trips", e);
                warnings.add("Driver trips import failed: " + e.getMessage());
                totalFailed++;
            }

            // Import account charges
            try {
                JSONArray chargesData = taxiCallerService.generateAccountJobReports(startDate, endDate);
                var chargesResult = accountChargeImportService.importAccountJobReports(chargesData);
                totalRecords += chargesResult.getTotalRecords();
                totalSuccess += chargesResult.getSuccessCount();
                totalFailed += chargesResult.getErrorCount();
                log.info("Imported {} account charges (success: {}, failed: {})",
                        chargesResult.getTotalRecords(), chargesResult.getSuccessCount(), chargesResult.getErrorCount());
            } catch (Exception e) {
                log.error("Failed to import account charges", e);
                warnings.add("Account charges import failed: " + e.getMessage());
                totalFailed++;
            }

            // Determine overall status
            ExecutionStatus status;
            if (totalFailed > 0 && totalSuccess > 0) {
                status = ExecutionStatus.PARTIAL;
            } else if (totalFailed > 0) {
                status = ExecutionStatus.FAILED;
            } else {
                status = ExecutionStatus.SUCCESS;
            }

            return resultBuilder
                    .endTime(LocalDateTime.now())
                    .status(status)
                    .recordsProcessed(totalRecords)
                    .recordsSuccess(totalSuccess)
                    .recordsFailed(totalFailed)
                    .warnings(warnings)
                    .build();

        } catch (Exception e) {
            log.error("TaxiCaller plugin execution failed", e);

            return resultBuilder
                    .endTime(LocalDateTime.now())
                    .status(ExecutionStatus.FAILED)
                    .recordsProcessed(totalRecords)
                    .recordsSuccess(totalSuccess)
                    .recordsFailed(totalFailed)
                    .error(e.getMessage())
                    .warnings(warnings)
                    .build();
        }
    }

    @Override
    public void validateConfiguration(PluginConfig config) throws PluginException {
        // Configuration is stored in tenant_config table for now
        // In future, this would validate the plugin_config JSON

        String configData = config.getConfigData();
        if (configData == null || configData.isBlank()) {
            throw new PluginException("Configuration data is required");
        }

        // Basic validation - would parse JSON and check required fields in production
        log.info("TaxiCaller configuration validated for tenant: {}", config.getTenantId());
    }
}
