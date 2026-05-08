package com.taxi.web.controller;

import com.taxi.domain.dbmigration.DbMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/db-migration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class DbMigrationController {

    private final DbMigrationService migrationService;

    /**
     * Test connection to external DB and list databases.
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connect(@RequestBody Map<String, Object> body) {
        try {
            String host = (String) body.get("host");
            int port = body.containsKey("port") ? ((Number) body.get("port")).intValue() : 3306;
            String username = (String) body.get("username");
            String password = (String) body.get("password");

            List<String> databases = migrationService.listDatabases(host, port, username, password);

            return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "databases", databases
            ));
        } catch (Exception e) {
            log.error("Failed to connect to external DB: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("connected", false, "error", "Connection failed: " + e.getMessage()));
        }
    }

    /**
     * List tables in a specific external database.
     */
    @PostMapping("/tables/external")
    public ResponseEntity<?> listExternalTables(@RequestBody Map<String, Object> body) {
        try {
            String host = (String) body.get("host");
            int port = body.containsKey("port") ? ((Number) body.get("port")).intValue() : 3306;
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String database = (String) body.get("database");

            List<String> tables = migrationService.listTables(host, port, username, password, database);
            return ResponseEntity.ok(Map.of("tables", tables));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List columns for an external table.
     */
    @PostMapping("/columns/external")
    public ResponseEntity<?> listExternalColumns(@RequestBody Map<String, Object> body) {
        try {
            String host = (String) body.get("host");
            int port = body.containsKey("port") ? ((Number) body.get("port")).intValue() : 3306;
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String database = (String) body.get("database");
            String table = (String) body.get("table");

            List<Map<String, String>> columns = migrationService.listExternalColumns(host, port, username, password, database, table);
            return ResponseEntity.ok(Map.of("columns", columns));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List tables in the local database.
     */
    @GetMapping("/tables/local")
    public ResponseEntity<?> listLocalTables() {
        try {
            List<String> tables = migrationService.listLocalTables();
            return ResponseEntity.ok(Map.of("tables", tables));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List columns for a local table.
     */
    @GetMapping("/columns/local")
    public ResponseEntity<?> listLocalColumns(@RequestParam String table) {
        try {
            List<Map<String, String>> columns = migrationService.listLocalColumns(table);
            return ResponseEntity.ok(Map.of("columns", columns));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Fetch sample data from external table with optional filter.
     */
    @PostMapping("/sample-data")
    public ResponseEntity<?> fetchSampleData(@RequestBody Map<String, Object> body) {
        try {
            String host = (String) body.get("host");
            int port = body.containsKey("port") ? ((Number) body.get("port")).intValue() : 3306;
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String database = (String) body.get("database");
            String table = (String) body.get("table");
            int limit = body.containsKey("limit") ? ((Number) body.get("limit")).intValue() : 20;

            @SuppressWarnings("unchecked")
            List<Map<String, String>> filters = (List<Map<String, String>>) body.get("filters");

            Map<String, Object> result = migrationService.fetchSampleData(
                    host, port, username, password, database, table, filters, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Preview migration with mappings applied.
     */
    @PostMapping("/preview")
    public ResponseEntity<?> previewMigration(@RequestBody Map<String, Object> body) {
        try {
            String host = (String) body.get("host");
            int port = body.containsKey("port") ? ((Number) body.get("port")).intValue() : 3306;
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String database = (String) body.get("database");
            String sourceTable = (String) body.get("sourceTable");
            String destTable = (String) body.get("destTable");

            @SuppressWarnings("unchecked")
            Map<String, List<String>> columnMappings = (Map<String, List<String>>) body.get("columnMappings");
            @SuppressWarnings("unchecked")
            Map<String, String> defaultValues = (Map<String, String>) body.get("defaultValues");
            @SuppressWarnings("unchecked")
            Map<String, String> conflictResolution = (Map<String, String>) body.get("conflictResolution");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> filters = (List<Map<String, String>>) body.get("filters");

            Map<String, Object> result = migrationService.previewMigration(
                    host, port, username, password, database, sourceTable, destTable,
                    columnMappings, defaultValues, conflictResolution, filters);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Execute the migration (IMPORT: External -> Local).
     */
    @PostMapping("/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> executeMigration(@RequestBody Map<String, Object> body) {
        try {
            String host = (String) body.get("host");
            int port = body.containsKey("port") ? ((Number) body.get("port")).intValue() : 3306;
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String database = (String) body.get("database");
            String sourceTable = (String) body.get("sourceTable");
            String destTable = (String) body.get("destTable");

            @SuppressWarnings("unchecked")
            Map<String, List<String>> columnMappings = (Map<String, List<String>>) body.get("columnMappings");
            @SuppressWarnings("unchecked")
            Map<String, String> defaultValues = (Map<String, String>) body.get("defaultValues");
            @SuppressWarnings("unchecked")
            Map<String, String> conflictResolution = (Map<String, String>) body.get("conflictResolution");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> filters = (List<Map<String, String>>) body.get("filters");

            log.info("Executing migration: {}:{}/{}.{} -> local.{}",
                    host, port, database, sourceTable, destTable);

            Map<String, Object> result = migrationService.executeMigration(
                    host, port, username, password, database, sourceTable, destTable,
                    columnMappings, defaultValues, conflictResolution, filters);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Migration failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Migration failed: " + e.getMessage()));
        }
    }

    // ========== EXPORT ENDPOINTS (Local -> External) ==========

    /**
     * Get tables from local database for export.
     */
    @GetMapping("/local-tables")
    public ResponseEntity<?> getLocalTablesForExport() {
        try {
            List<String> tables = migrationService.listLocalTables();
            return ResponseEntity.ok(Map.of("tables", tables));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get tables from external database (simplified GET endpoint).
     */
    @GetMapping("/tables")
    public ResponseEntity<?> getTables(@RequestParam String database) {
        // Note: This requires connection params stored in session or passed differently
        // For now, frontend should use POST /tables/external with full connection details
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Use POST /tables/external with connection params"));
    }

    /**
     * Get columns from local table for export.
     */
    @GetMapping("/local-columns")
    public ResponseEntity<?> getLocalColumnsForExport(@RequestParam String table) {
        try {
            List<Map<String, String>> columns = migrationService.listLocalColumns(table);
            return ResponseEntity.ok(Map.of("columns", columns));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get columns from external table for export.
     */
    @GetMapping("/external-columns")
    public ResponseEntity<?> getExternalColumnsForExport(
            @RequestParam String database,
            @RequestParam String table) {
        // Note: Requires connection params - use POST /columns/external instead
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Use POST /columns/external with connection params"));
    }

    /**
     * Count source records that will be exported based on filters.
     */
    @PostMapping("/count-export")
    public ResponseEntity<?> countExportRecords(@RequestBody Map<String, Object> body) {
        try {
            String sourceTable = (String) body.get("sourceTable");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> filters = (List<Map<String, String>>) body.get("filters");

            Map<String, Object> result = migrationService.countExportRecords(sourceTable, filters);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Preview local data before export with optional filters.
     * Shows destination table structure with mapped columns and transformations applied.
     */
    @PostMapping("/preview-export")
    public ResponseEntity<?> previewExportData(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> connection = (Map<String, Object>) body.get("connection");
            String host = (String) connection.get("host");
            int port = connection.containsKey("port") ? ((Number) connection.get("port")).intValue() : 3306;
            String username = (String) connection.get("username");
            String password = (String) connection.get("password");

            String sourceDatabase = (String) body.get("sourceDatabase");
            String sourceTable = (String) body.get("sourceTable");
            String destTable = (String) body.get("destTable");
            int limit = body.containsKey("limit") ? ((Number) body.get("limit")).intValue() : 10;

            @SuppressWarnings("unchecked")
            List<Map<String, String>> columnMappings = (List<Map<String, String>>) body.get("columnMappings");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> filters = (List<Map<String, String>>) body.get("filters");

            Map<String, Object> result = migrationService.previewLocalData(
                    host, port, username, password, sourceDatabase,
                    sourceTable, destTable, columnMappings, filters, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Execute export (Local -> External).
     * Supports one-to-many column mappings (array of {source, dest} objects).
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> executeExport(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> connection = (Map<String, Object>) body.get("connection");
            String host = (String) connection.get("host");
            int port = connection.containsKey("port") ? ((Number) connection.get("port")).intValue() : 3306;
            String username = (String) connection.get("username");
            String password = (String) connection.get("password");

            String sourceDatabase = (String) body.get("sourceDatabase");
            String sourceTable = (String) body.get("sourceTable");
            String destTable = (String) body.get("destTable");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> columnMappings = (List<Map<String, String>>) body.get("columnMappings");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> filters = (List<Map<String, String>>) body.get("filters");

            log.info("Executing export: local.{} -> {}:{}/{}.{} (mappings: {}, filters: {})",
                    sourceTable, host, port, sourceDatabase, destTable,
                    columnMappings != null ? columnMappings.size() : 0,
                    filters != null ? filters.size() : 0);

            Map<String, Object> result = migrationService.executeExport(
                    host, port, username, password, sourceDatabase,
                    sourceTable, destTable, columnMappings, filters);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Export failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Export failed: " + e.getMessage()));
        }
    }
}
