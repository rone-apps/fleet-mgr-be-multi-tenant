package com.taxi.domain.dbmigration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DbMigrationService {

    private final DataSource localDataSource;

    /**
     * Test connection and list databases on external MySQL server.
     */
    public List<String> listDatabases(String host, int port, String username, String password) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000";
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            List<String> databases = new ArrayList<>();
            try (ResultSet rs = conn.getMetaData().getCatalogs()) {
                while (rs.next()) {
                    String db = rs.getString(1);
                    // Skip system databases
                    if (!Set.of("information_schema", "mysql", "performance_schema", "sys").contains(db.toLowerCase())) {
                        databases.add(db);
                    }
                }
            }
            Collections.sort(databases);
            return databases;
        }
    }

    /**
     * List tables in a specific database on the external server.
     */
    public List<String> listTables(String host, int port, String username, String password, String database) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000";
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = conn.getMetaData().getTables(database, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            Collections.sort(tables);
            return tables;
        }
    }

    /**
     * List columns for a table on the external server.
     */
    public List<Map<String, String>> listExternalColumns(String host, int port, String username, String password,
                                                          String database, String table) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000";
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            return getColumnsFromConnection(conn, database, table);
        }
    }

    /**
     * List tables in the local database.
     */
    public List<String> listLocalTables() throws SQLException {
        try (Connection conn = localDataSource.getConnection()) {
            String catalog = conn.getCatalog();
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = conn.getMetaData().getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            Collections.sort(tables);
            return tables;
        }
    }

    /**
     * List columns for a table in the local database.
     */
    public List<Map<String, String>> listLocalColumns(String table) throws SQLException {
        try (Connection conn = localDataSource.getConnection()) {
            return getColumnsFromConnection(conn, conn.getCatalog(), table);
        }
    }

    /**
     * Fetch sample rows from external table with optional filter.
     */
    public Map<String, Object> fetchSampleData(String host, int port, String username, String password,
                                                String database, String table,
                                                List<Map<String, String>> filters,
                                                int limit) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000";
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String whereClause = buildWhereClause(filters);

            // Count total matching rows
            String countSql = "SELECT COUNT(*) FROM `" + sanitizeIdentifier(table) + "`" + whereClause;
            long totalRows;
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                setFilterParams(countStmt, filters, 1);
                try (ResultSet rs = countStmt.executeQuery()) {
                    rs.next();
                    totalRows = rs.getLong(1);
                }
            }

            // Fetch sample rows
            String sql = "SELECT * FROM `" + sanitizeIdentifier(table) + "`" + whereClause + " LIMIT ?";
            List<Map<String, Object>> rows = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int paramIdx = setFilterParams(stmt, filters, 1);
                stmt.setInt(paramIdx, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        rows.add(row);
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalRows", totalRows);
            result.put("sampleRows", rows);
            result.put("sampleCount", rows.size());
            return result;
        }
    }

    /**
     * Preview migration: apply column mappings to sample data and show what would be inserted.
     */
    public Map<String, Object> previewMigration(String host, int port, String username, String password,
                                                 String database, String sourceTable, String destTable,
                                                 Map<String, List<String>> columnMappings,
                                                 Map<String, String> defaultValues,
                                                 Map<String, String> conflictResolution,
                                                 List<Map<String, String>> filters) throws SQLException {

        // Fetch sample from source
        Map<String, Object> sampleData = fetchSampleData(host, port, username, password,
                database, sourceTable, filters, 10);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sourceRows = (List<Map<String, Object>>) sampleData.get("sampleRows");

        // Get destination columns
        List<Map<String, String>> destColumns = listLocalColumns(destTable);
        Set<String> destColumnNames = destColumns.stream()
                .map(c -> c.get("name"))
                .collect(Collectors.toSet());

        // Transform sample rows using mappings (one source can map to multiple dest cols)
        List<Map<String, Object>> previewRows = new ArrayList<>();
        for (Map<String, Object> sourceRow : sourceRows) {
            Map<String, Object> destRow = new LinkedHashMap<>();

            for (Map.Entry<String, List<String>> mapping : columnMappings.entrySet()) {
                String sourceCol = mapping.getKey();
                List<String> destCols = mapping.getValue();
                if (destCols == null) continue;
                Object value = sourceRow.get(sourceCol);
                for (String destCol : destCols) {
                    if (destCol != null && !destCol.isEmpty() && destColumnNames.contains(destCol)) {
                        destRow.put(destCol, value);
                    }
                }
            }

            // Apply default values for unmapped columns
            if (defaultValues != null) {
                for (Map.Entry<String, String> def : defaultValues.entrySet()) {
                    String col = def.getKey();
                    if (!destRow.containsKey(col) || destRow.get(col) == null) {
                        destRow.put(col, def.getValue());
                    }
                }
            }

            previewRows.add(destRow);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSourceRows", sampleData.get("totalRows"));
        result.put("previewRows", previewRows);
        result.put("mappedColumns", columnMappings.size());
        result.put("destinationTable", destTable);
        return result;
    }

    /**
     * Execute the migration: read from source, transform, insert into destination.
     */
    public Map<String, Object> executeMigration(String host, int port, String username, String password,
                                                 String database, String sourceTable, String destTable,
                                                 Map<String, List<String>> columnMappings,
                                                 Map<String, String> defaultValues,
                                                 Map<String, String> conflictResolution,
                                                 List<Map<String, String>> filters) throws SQLException {

        String externalUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000";

        int successCount = 0;
        int errorCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();
        List<String> skippedDetails = new ArrayList<>();

        // Get destination columns for type info
        List<Map<String, String>> destColumns = listLocalColumns(destTable);
        Map<String, String> destColumnTypes = destColumns.stream()
                .collect(Collectors.toMap(c -> c.get("name"), c -> c.get("type")));

        // Build destination column list from mappings (one source can map to multiple dest cols)
        // sourceCols and destCols are parallel lists: sourceCols[i] feeds into destCols[i]
        // Deduplicate: if multiple sources map to the same dest col, last one wins
        LinkedHashMap<String, String> destToSource = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> mapping : columnMappings.entrySet()) {
            String srcCol = mapping.getKey();
            List<String> dstList = mapping.getValue();
            if (dstList == null) continue;
            for (String dst : dstList) {
                if (dst != null && !dst.isEmpty() && destColumnTypes.containsKey(dst)) {
                    destToSource.put(dst, srcCol);
                }
            }
        }
        List<String> destCols = new ArrayList<>(destToSource.keySet());
        List<String> sourceCols = new ArrayList<>(destToSource.values());

        // Add default value columns not already in mappings
        List<String> defaultCols = new ArrayList<>();
        List<String> defaultVals = new ArrayList<>();
        if (defaultValues != null) {
            for (Map.Entry<String, String> def : defaultValues.entrySet()) {
                if (!destCols.contains(def.getKey()) && destColumnTypes.containsKey(def.getKey())) {
                    defaultCols.add(def.getKey());
                    defaultVals.add(def.getValue());
                }
            }
        }

        // Build INSERT SQL
        List<String> allDestCols = new ArrayList<>(destCols);
        allDestCols.addAll(defaultCols);

        String placeholders = allDestCols.stream().map(c -> "?").collect(Collectors.joining(", "));
        String colNames = allDestCols.stream().map(c -> "`" + sanitizeIdentifier(c) + "`").collect(Collectors.joining(", "));

        // Handle conflict resolution - build ON DUPLICATE KEY UPDATE if needed
        String insertSql;
        boolean hasConflictResolution = conflictResolution != null && !conflictResolution.isEmpty();
        if (hasConflictResolution) {
            StringBuilder updateClause = new StringBuilder();
            boolean first = true;
            for (String col : allDestCols) {
                String resolution = conflictResolution.getOrDefault(col, "source");
                if ("source".equals(resolution)) {
                    if (!first) updateClause.append(", ");
                    updateClause.append("`").append(sanitizeIdentifier(col)).append("` = VALUES(`")
                            .append(sanitizeIdentifier(col)).append("`)");
                    first = false;
                }
            }
            if (updateClause.length() > 0) {
                insertSql = "INSERT INTO `" + sanitizeIdentifier(destTable) + "` (" + colNames + ") VALUES (" + placeholders + ") ON DUPLICATE KEY UPDATE " + updateClause;
            } else {
                insertSql = "INSERT IGNORE INTO `" + sanitizeIdentifier(destTable) + "` (" + colNames + ") VALUES (" + placeholders + ")";
            }
        } else {
            // Default: skip duplicates rather than failing
            insertSql = "INSERT IGNORE INTO `" + sanitizeIdentifier(destTable) + "` (" + colNames + ") VALUES (" + placeholders + ")";
        }

        log.info("Migration SQL: {}", insertSql);

        // Build source SELECT
        String whereClause = buildWhereClause(filters);
        String sourceSql = "SELECT * FROM `" + sanitizeIdentifier(sourceTable) + "`" + whereClause;

        try (Connection sourceConn = DriverManager.getConnection(externalUrl, username, password);
             Connection destConn = localDataSource.getConnection()) {

            destConn.setAutoCommit(false);

            try (PreparedStatement sourceStmt = sourceConn.prepareStatement(sourceSql)) {
                setFilterParams(sourceStmt, filters, 1);

                try (ResultSet rs = sourceStmt.executeQuery();
                     PreparedStatement insertStmt = destConn.prepareStatement(insertSql)) {

                    int batchSize = 0;
                    List<String> batchRowSummaries = new ArrayList<>();

                    while (rs.next()) {
                        try {
                            int paramIdx = 1;

                            // Build a summary string for this row (first few mapped cols)
                            StringBuilder rowSummary = new StringBuilder();
                            for (int ci = 0; ci < Math.min(sourceCols.size(), 5); ci++) {
                                Object val = rs.getObject(sourceCols.get(ci));
                                if (ci > 0) rowSummary.append(", ");
                                rowSummary.append(destCols.get(ci)).append("=").append(val);
                            }

                            // Set mapped column values
                            for (int ci = 0; ci < sourceCols.size(); ci++) {
                                Object val = rs.getObject(sourceCols.get(ci));
                                String destCol = destCols.get(ci);
                                if (val == null) {
                                    insertStmt.setNull(paramIdx++, Types.NULL);
                                } else {
                                    insertStmt.setObject(paramIdx++, transformValue(val, destCol));
                                }
                            }

                            // Set default values
                            for (String defVal : defaultVals) {
                                if (defVal == null || defVal.isEmpty()) {
                                    insertStmt.setNull(paramIdx++, Types.NULL);
                                } else {
                                    insertStmt.setString(paramIdx++, defVal);
                                }
                            }

                            insertStmt.addBatch();
                            batchRowSummaries.add(rowSummary.toString());
                            batchSize++;

                            if (batchSize >= 500) {
                                int[] results = insertStmt.executeBatch();
                                for (int ri = 0; ri < results.length; ri++) {
                                    if (results[ri] > 0 || results[ri] == Statement.SUCCESS_NO_INFO) {
                                        successCount++;
                                    } else {
                                        skippedCount++;
                                        String detail = ri < batchRowSummaries.size() ? batchRowSummaries.get(ri) : "unknown";
                                        log.info("Skipped duplicate row: {}", detail);
                                        if (skippedDetails.size() < 100) {
                                            skippedDetails.add(detail);
                                        }
                                    }
                                }
                                batchSize = 0;
                                batchRowSummaries.clear();
                            }

                        } catch (Exception e) {
                            errorCount++;
                            if (errors.size() < 50) {
                                errors.add("Row error: " + e.getMessage());
                            }
                        }
                    }

                    // Execute remaining batch
                    if (batchSize > 0) {
                        int[] results = insertStmt.executeBatch();
                        for (int ri = 0; ri < results.length; ri++) {
                            if (results[ri] > 0 || results[ri] == Statement.SUCCESS_NO_INFO) {
                                successCount++;
                            } else {
                                skippedCount++;
                                String detail = ri < batchRowSummaries.size() ? batchRowSummaries.get(ri) : "unknown";
                                log.info("Skipped duplicate row: {}", detail);
                                if (skippedDetails.size() < 100) {
                                    skippedDetails.add(detail);
                                }
                            }
                        }
                    }
                }

                destConn.commit();

            } catch (Exception e) {
                destConn.rollback();
                throw e;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("skippedCount", skippedCount);
        result.put("totalProcessed", successCount + errorCount + skippedCount);
        result.put("errors", errors.subList(0, Math.min(errors.size(), 20)));
        result.put("skippedRows", skippedDetails.subList(0, Math.min(skippedDetails.size(), 50)));
        result.put("status", errorCount == 0 ? "SUCCESS" : (successCount > 0 ? "PARTIAL" : "FAILED"));

        log.info("Migration complete: {} success, {} errors, {} skipped",
                successCount, errorCount, skippedCount);

        return result;
    }

    // --- Helpers ---

    private List<Map<String, String>> getColumnsFromConnection(Connection conn, String catalog, String table) throws SQLException {
        List<Map<String, String>> columns = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getColumns(catalog, null, table, "%")) {
            while (rs.next()) {
                Map<String, String> col = new LinkedHashMap<>();
                col.put("name", rs.getString("COLUMN_NAME"));
                col.put("type", rs.getString("TYPE_NAME"));
                col.put("size", String.valueOf(rs.getInt("COLUMN_SIZE")));
                col.put("nullable", rs.getString("IS_NULLABLE"));
                col.put("defaultValue", rs.getString("COLUMN_DEF"));
                columns.add(col);
            }
        }
        return columns;
    }

    private String buildWhereClause(List<Map<String, String>> filters) {
        if (filters == null || filters.isEmpty()) return "";
        List<String> conditions = new ArrayList<>();
        for (Map<String, String> filter : filters) {
            String col = filter.get("column");
            String op = resolveOperator(filter.get("operator"));
            String val = filter.get("value");
            if (col != null && !col.isEmpty() && val != null && !val.isEmpty()) {
                conditions.add("`" + sanitizeIdentifier(col) + "` " + op + " ?");
            }
        }
        if (conditions.isEmpty()) return "";
        return " WHERE " + String.join(" AND ", conditions);
    }

    private int setFilterParams(PreparedStatement stmt, List<Map<String, String>> filters, int startIdx) throws SQLException {
        int idx = startIdx;
        if (filters == null) return idx;
        for (Map<String, String> filter : filters) {
            String col = filter.get("column");
            String val = filter.get("value");
            if (col != null && !col.isEmpty() && val != null && !val.isEmpty()) {
                stmt.setString(idx++, val);
            }
        }
        return idx;
    }

    /**
     * Transform values based on destination column.
     * e.g. cab_number "1B" -> "1" (strip alphabets, keep only digits)
     */
    private Object transformValue(Object val, String destColumn) {
        if (val == null) return null;
        String destLower = destColumn.toLowerCase();
        // Cab number columns: strip all non-digit characters
        if (destLower.contains("cab") && (destLower.contains("number") || destLower.contains("num") || destLower.contains("id"))) {
            String str = val.toString().replaceAll("[^0-9]", "");
            return str.isEmpty() ? null : str;
        }
        return val;
    }

    /**
     * Transform value based on destination data type.
     * Handles conversions like DATETIME -> TIME, DATETIME -> DATE, etc.
     */
    private Object transformValueForType(Object value, String destType, String destColumn) {
        if (value == null) return null;

        String typeLower = destType != null ? destType.toLowerCase() : "";

        try {
            // DATETIME/TIMESTAMP -> TIME conversion
            if (typeLower.contains("time") && !typeLower.contains("datetime") && !typeLower.contains("timestamp")) {
                if (value instanceof java.sql.Timestamp) {
                    java.sql.Timestamp ts = (java.sql.Timestamp) value;
                    return new java.sql.Time(ts.getTime());
                } else if (value instanceof java.util.Date) {
                    java.util.Date date = (java.util.Date) value;
                    return new java.sql.Time(date.getTime());
                }
            }

            // DATETIME/TIMESTAMP -> DATE conversion
            if (typeLower.equals("date")) {
                if (value instanceof java.sql.Timestamp) {
                    java.sql.Timestamp ts = (java.sql.Timestamp) value;
                    return new java.sql.Date(ts.getTime());
                } else if (value instanceof java.util.Date) {
                    java.util.Date date = (java.util.Date) value;
                    return new java.sql.Date(date.getTime());
                }
            }

            // Apply legacy cab_number transformation
            return transformValue(value, destColumn);

        } catch (Exception e) {
            log.warn("Failed to transform value for column {} (type: {}): {}", destColumn, destType, e.getMessage());
            return value;
        }
    }

    private String sanitizeIdentifier(String identifier) {
        // Only allow alphanumeric and underscores to prevent SQL injection
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
    }

    private String resolveOperator(String operator) {
        if (operator == null) return "=";
        return switch (operator) {
            case "gt", ">" -> ">";
            case "gte", ">=" -> ">=";
            case "lt", "<" -> "<";
            case "lte", "<=" -> "<=";
            case "eq", "=" -> "=";
            case "neq", "!=" -> "!=";
            case "like" -> "LIKE";
            default -> "=";
        };
    }

    // ========== EXPORT METHODS (Local -> External) ==========

    /**
     * Count source records that will be exported based on filters.
     */
    public Map<String, Object> countExportRecords(String sourceTable, List<Map<String, String>> filters) throws Exception {
        String sanitizedSource = sanitizeIdentifier(sourceTable);
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + sanitizedSource);

        log.info("Counting records from {} with {} filters", sourceTable, filters != null ? filters.size() : 0);
        if (filters != null && !filters.isEmpty()) {
            for (Map<String, String> filter : filters) {
                log.info("Filter: column={}, operator={}, value={}",
                    filter.get("column"), filter.get("operator"), filter.get("value"));
            }
        }

        // Build WHERE clause from filters
        List<String> conditions = new ArrayList<>();
        List<String> paramValues = new ArrayList<>();

        if (filters != null && !filters.isEmpty()) {
            for (Map<String, String> filter : filters) {
                String col = filter.get("column");
                String operator = filter.get("operator");
                String value = filter.get("value");

                // Only add condition if we have all three: column, operator, AND value
                if (col != null && !col.trim().isEmpty() &&
                    operator != null && !operator.trim().isEmpty() &&
                    value != null && !value.trim().isEmpty()) {
                    conditions.add(sanitizeIdentifier(col) + " " + resolveOperator(operator) + " ?");
                    paramValues.add(value);
                } else {
                    log.warn("Skipping incomplete filter: col={}, op={}, val={}", col, operator, value);
                }
            }
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        log.info("Count SQL: {}", sql);
        log.info("Parameters: {}", paramValues);

        try (Connection conn = localDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Set filter parameters
            for (int i = 0; i < paramValues.size(); i++) {
                stmt.setString(i + 1, paramValues.get(i));
                log.debug("Setting param {}: {}", i + 1, paramValues.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                log.info("Found {} records to export from {} (with {} active filters)",
                    count, sourceTable, paramValues.size());

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("count", count);
                result.put("sourceTable", sourceTable);
                result.put("query", sql.toString());
                result.put("parameters", paramValues);
                return result;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("count", 0);
            result.put("sourceTable", sourceTable);
            result.put("query", sql.toString());
            result.put("parameters", paramValues);
            return result;
        }
    }

    /**
     * Preview local data before export with optional filters.
     * Shows how destination table rows will look with mapped columns and transformations applied.
     */
    public Map<String, Object> previewLocalData(
            String host, int port, String username, String password, String targetDatabase,
            String sourceTable, String destTable,
            List<Map<String, String>> columnMappings,
            List<Map<String, String>> filters, int limit) throws Exception {

        String sanitizedSource = sanitizeIdentifier(sourceTable);
        String sanitizedDest = sanitizeIdentifier(destTable);

        // External DB connection for metadata check
        String extDbUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                host, port, targetDatabase);

        // Get destination column metadata to detect auto-increment and data types
        Map<String, Map<String, Object>> destColumnMeta = new LinkedHashMap<>();
        try (Connection extConn = DriverManager.getConnection(extDbUrl, username, password)) {
            DatabaseMetaData meta = extConn.getMetaData();
            ResultSet rsColumns = meta.getColumns(targetDatabase, null, sanitizedDest, null);
            while (rsColumns.next()) {
                String colName = rsColumns.getString("COLUMN_NAME");
                String isAutoInc = rsColumns.getString("IS_AUTOINCREMENT");
                String dataType = rsColumns.getString("TYPE_NAME");

                Map<String, Object> info = new HashMap<>();
                info.put("autoIncrement", "YES".equalsIgnoreCase(isAutoInc));
                info.put("dataType", dataType);
                destColumnMeta.put(colName.toLowerCase(), info);
            }
        }

        // Build SELECT columns from mappings (skip auto-increment)
        List<String> selectColumns = new ArrayList<>();
        List<String> destColumns = new ArrayList<>();

        for (Map<String, String> mapping : columnMappings) {
            String srcCol = mapping.get("source");
            String destCol = mapping.get("dest");

            if (srcCol == null || srcCol.trim().isEmpty() || destCol == null || destCol.trim().isEmpty()) {
                continue;
            }

            String sanitizedSrcCol = sanitizeIdentifier(srcCol);
            String sanitizedDestCol = sanitizeIdentifier(destCol);

            // Check if destination column is auto-increment (skip if it is)
            Map<String, Object> destInfo = destColumnMeta.get(sanitizedDestCol.toLowerCase());
            if (destInfo != null && Boolean.TRUE.equals(destInfo.get("autoIncrement"))) {
                log.info("Skipping auto-increment column in preview: {}", sanitizedDestCol);
                continue;
            }

            selectColumns.add(sanitizedSrcCol);
            destColumns.add(sanitizedDestCol);
        }

        if (selectColumns.isEmpty()) {
            throw new IllegalArgumentException("No valid column mappings provided");
        }

        String selectClause = String.join(", ", selectColumns);

        // Build WHERE clause with proper validation (same logic as countExportRecords)
        List<String> conditions = new ArrayList<>();
        List<String> paramValues = new ArrayList<>();

        if (filters != null && !filters.isEmpty()) {
            for (Map<String, String> filter : filters) {
                String col = filter.get("column");
                String operator = filter.get("operator");
                String value = filter.get("value");

                // Only add condition if we have all three: column, operator, AND value
                if (col != null && !col.trim().isEmpty() &&
                    operator != null && !operator.trim().isEmpty() &&
                    value != null && !value.trim().isEmpty()) {
                    conditions.add(sanitizeIdentifier(col) + " " + resolveOperator(operator) + " ?");
                    paramValues.add(value);
                } else {
                    log.warn("Skipping incomplete filter in preview: col={}, op={}, val={}", col, operator, value);
                }
            }
        }

        // Build SELECT SQL
        StringBuilder sql = new StringBuilder("SELECT " + selectClause + " FROM " + sanitizedSource);
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" LIMIT ?");

        log.info("Preview SQL: {}", sql);
        log.info("Preview Parameters: {}", paramValues);

        try (Connection conn = localDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            // Set filter parameters
            for (String value : paramValues) {
                stmt.setString(paramIdx++, value);
            }
            // Set limit
            stmt.setInt(paramIdx, limit);

            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();

                // Map each source column to destination column with transformations
                for (int i = 0; i < selectColumns.size(); i++) {
                    Object value = rs.getObject(i + 1);
                    String destCol = destColumns.get(i);

                    // Apply type transformations
                    Map<String, Object> destInfo = destColumnMeta.get(destCol.toLowerCase());
                    if (destInfo != null) {
                        String destType = (String) destInfo.get("dataType");
                        value = transformValueForType(value, destType, destCol);
                    }

                    // Use destination column name (this is how it will appear in dest table)
                    row.put(destCol, value);
                }
                rows.add(row);
            }

            return Map.of(
                    "rows", rows,
                    "count", rows.size(),
                    "columns", destColumns  // Return destination column names for frontend
            );
        }
    }

    /**
     * Execute export from local FareFlow DB to external database with optional filters.
     * Supports one-to-many column mappings (one source column can map to multiple destination columns).
     */
    public Map<String, Object> executeExport(
            String host, int port, String username, String password, String targetDatabase,
            String sourceTable, String destTable, List<Map<String, String>> columnMappings, List<Map<String, String>> filters) throws Exception {

        log.info("Starting export: local.{} -> {}:{}/{}.{}",
                sourceTable, host, port, targetDatabase, destTable);

        String sanitizedSource = sanitizeIdentifier(sourceTable);
        String sanitizedDest = sanitizeIdentifier(destTable);

        // External DB connection for metadata check
        String extDbUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                host, port, targetDatabase);

        // Get destination column metadata to detect auto-increment and data types
        Map<String, Map<String, Object>> destColumnMeta = new LinkedHashMap<>();
        try (Connection extConn = DriverManager.getConnection(extDbUrl, username, password)) {
            DatabaseMetaData meta = extConn.getMetaData();
            ResultSet rsColumns = meta.getColumns(targetDatabase, null, sanitizedDest, null);
            while (rsColumns.next()) {
                String colName = rsColumns.getString("COLUMN_NAME");
                String isAutoInc = rsColumns.getString("IS_AUTOINCREMENT");
                String dataType = rsColumns.getString("TYPE_NAME");

                Map<String, Object> info = new HashMap<>();
                info.put("autoIncrement", "YES".equalsIgnoreCase(isAutoInc));
                info.put("dataType", dataType);
                destColumnMeta.put(colName.toLowerCase(), info);
            }
        }

        // Build SELECT columns from mappings (support one-to-many)
        List<String> selectColumns = new ArrayList<>();
        List<String> destColumns = new ArrayList<>();

        for (Map<String, String> mapping : columnMappings) {
            String srcCol = mapping.get("source");
            String destCol = mapping.get("dest");

            // Skip empty or invalid mappings
            if (srcCol == null || srcCol.trim().isEmpty()) {
                log.warn("Skipping invalid source column: '{}'", srcCol);
                continue;
            }
            if (destCol == null || destCol.trim().isEmpty()) {
                log.warn("Skipping invalid destination column: '{}'", destCol);
                continue;
            }

            String sanitizedSrcCol = sanitizeIdentifier(srcCol);
            String sanitizedDestCol = sanitizeIdentifier(destCol);

            // Check if destination column is auto-increment (skip if it is)
            Map<String, Object> destInfo = destColumnMeta.get(sanitizedDestCol.toLowerCase());
            if (destInfo != null && Boolean.TRUE.equals(destInfo.get("autoIncrement"))) {
                log.info("Skipping auto-increment column: {}", sanitizedDestCol);
                continue;
            }

            selectColumns.add(sanitizedSrcCol);
            destColumns.add(sanitizedDestCol);
            log.debug("Column mapping: {} -> {} (type: {})", sanitizedSrcCol, sanitizedDestCol,
                destInfo != null ? destInfo.get("dataType") : "unknown");
        }

        if (selectColumns.isEmpty()) {
            throw new IllegalArgumentException("No valid column mappings provided");
        }

        log.info("Export column mappings: {} columns mapped", selectColumns.size());

        String selectClause = String.join(", ", selectColumns);
        String insertClause = String.join(", ", destColumns);
        String placeholders = String.join(", ", Collections.nCopies(destColumns.size(), "?"));

        // Build WHERE clause with proper validation (same logic as countExportRecords)
        List<String> conditions = new ArrayList<>();
        List<String> paramValues = new ArrayList<>();

        if (filters != null && !filters.isEmpty()) {
            for (Map<String, String> filter : filters) {
                String col = filter.get("column");
                String operator = filter.get("operator");
                String value = filter.get("value");

                // Only add condition if we have all three: column, operator, AND value
                if (col != null && !col.trim().isEmpty() &&
                    operator != null && !operator.trim().isEmpty() &&
                    value != null && !value.trim().isEmpty()) {
                    conditions.add(sanitizeIdentifier(col) + " " + resolveOperator(operator) + " ?");
                    paramValues.add(value);
                } else {
                    log.warn("Skipping incomplete filter in export: col={}, op={}, val={}", col, operator, value);
                }
            }
        }

        // Build SELECT SQL
        StringBuilder selectSqlBuilder = new StringBuilder("SELECT " + selectClause + " FROM " + sanitizedSource);
        if (!conditions.isEmpty()) {
            selectSqlBuilder.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        String selectSql = selectSqlBuilder.toString();
        String insertSql = "INSERT INTO " + sanitizedDest + " (" + insertClause + ") VALUES (" + placeholders + ")";
        log.info("Export SELECT SQL: {}", selectSql);
        log.info("Export INSERT SQL: {}", insertSql);
        log.info("Export Parameters: {}", paramValues);

        // Count total records to export
        int totalRecords = 0;
        try {
            Map<String, Object> countResult = countExportRecords(sourceTable, filters);
            totalRecords = (int) countResult.get("count");
            log.info("Total records to export: {}", totalRecords);
        } catch (Exception e) {
            log.warn("Failed to count records: {}", e.getMessage());
        }

        int rowsExported = 0;
        int rowsFailed = 0;
        Map<String, Integer> failureReasons = new LinkedHashMap<>();
        List<String> sampleErrors = new ArrayList<>();
        int maxSampleErrors = 10;

        try (Connection localConn = localDataSource.getConnection();
             Connection extConn = DriverManager.getConnection(extDbUrl, username, password)) {

            // Read from local
            PreparedStatement selectStmt = localConn.prepareStatement(selectSql);

            // Set filter parameters
            for (int i = 0; i < paramValues.size(); i++) {
                selectStmt.setString(i + 1, paramValues.get(i));
                log.debug("Setting export param {}: {}", i + 1, paramValues.get(i));
            }

            ResultSet rs = selectStmt.executeQuery();

            // Write to external - individual row processing for detailed error tracking
            PreparedStatement insertStmt = extConn.prepareStatement(insertSql);
            extConn.setAutoCommit(true); // Auto-commit each row to isolate failures

            int processedCount = 0;
            while (rs.next()) {
                processedCount++;
                try {
                    // Set values for this row
                    for (int i = 1; i <= selectColumns.size(); i++) {
                        Object value = rs.getObject(i);
                        String destCol = destColumns.get(i - 1);

                        // Transform value based on destination column type
                        Map<String, Object> destInfo = destColumnMeta.get(destCol.toLowerCase());
                        if (destInfo != null) {
                            String destType = (String) destInfo.get("dataType");
                            value = transformValueForType(value, destType, destCol);
                        }

                        insertStmt.setObject(i, value);
                    }

                    // Execute insert for this row
                    insertStmt.executeUpdate();
                    rowsExported++;

                    // Log progress every 100 rows
                    if (processedCount % 100 == 0) {
                        log.info("Processed {} / {} rows (success: {}, failed: {})",
                                processedCount, totalRecords > 0 ? totalRecords : "?", rowsExported, rowsFailed);
                    }

                } catch (SQLException e) {
                    rowsFailed++;

                    // Categorize the error
                    String errorCategory = categorizeError(e);
                    failureReasons.merge(errorCategory, 1, Integer::sum);

                    // Store sample error messages
                    if (sampleErrors.size() < maxSampleErrors) {
                        sampleErrors.add(String.format("Row %d: %s", processedCount, e.getMessage()));
                    }

                    log.debug("Failed to insert row {}: {} - {}", processedCount, errorCategory, e.getMessage());
                }
            }

            log.info("Export completed: {} rows exported, {} failed out of {} total",
                    rowsExported, rowsFailed, processedCount);

            // Build detailed result
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("totalRecords", totalRecords > 0 ? totalRecords : processedCount);
            result.put("processedRecords", processedCount);
            result.put("successfulRecords", rowsExported);
            result.put("failedRecords", rowsFailed);
            result.put("sourceTable", sourceTable);
            result.put("destTable", destTable);

            if (!failureReasons.isEmpty()) {
                result.put("failureReasons", failureReasons);
            }
            if (!sampleErrors.isEmpty()) {
                result.put("sampleErrors", sampleErrors);
            }

            return result;

        } catch (Exception e) {
            log.error("Export failed: {}", e.getMessage(), e);
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Categorize SQL error for reporting.
     */
    private String categorizeError(SQLException e) {
        String message = e.getMessage().toLowerCase();
        int errorCode = e.getErrorCode();

        // MySQL error codes
        if (errorCode == 1062 || message.contains("duplicate")) {
            return "Duplicate Key";
        } else if (errorCode == 1452 || message.contains("foreign key")) {
            return "Foreign Key Constraint";
        } else if (errorCode == 1048 || message.contains("cannot be null")) {
            return "NULL Constraint Violation";
        } else if (errorCode == 1406 || message.contains("data too long")) {
            return "Data Too Long";
        } else if (errorCode == 1264 || message.contains("out of range")) {
            return "Value Out of Range";
        } else if (message.contains("constraint")) {
            return "Constraint Violation";
        } else if (message.contains("truncated")) {
            return "Data Truncation";
        } else {
            return "Other Error";
        }
    }
}
