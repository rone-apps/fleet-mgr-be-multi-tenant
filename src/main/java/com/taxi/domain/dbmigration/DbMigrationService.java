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
}
