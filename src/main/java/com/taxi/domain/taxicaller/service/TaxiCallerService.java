package com.taxi.domain.taxicaller.service;

import com.taxi.domain.tenant.exception.TenantConfigurationException;
import com.taxi.domain.tenant.service.TenantConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxiCallerService {

    private final TenantConfigService tenantConfigService;

    // Cache tokens per tenant to avoid fetching on every request
    private final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();

    private String getApiKey() {
        return tenantConfigService.getTaxicallerApiKey();
    }

    private int getCompanyId() {
        return tenantConfigService.getTaxicallerCompanyId();
    }

    private String getBaseUrl() {
        return tenantConfigService.getTaxicallerBaseUrl();
    }

    /**
     * Fetch a new JWT token from TaxiCaller API
     */
    private String fetchToken() {
        try {
            String urlString = getBaseUrl() + "/AdminService/v1/jwt/for-key?key=" + getApiKey() + "&sub=*&ttl=900";
            log.info("Fetching TaxiCaller token from: {}", getBaseUrl());
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorResponse = readErrorResponse(conn);
                log.error("Failed to get TaxiCaller token. HTTP {}: {}", responseCode, errorResponse);
                throw new RuntimeException("TaxiCaller authentication failed: " + errorResponse);
            }

            String response = readResponse(conn);
            if (response == null || response.isBlank() || !response.trim().startsWith("{")) {
                log.error("Invalid TaxiCaller token response: {}", response);
                throw new RuntimeException("Invalid response from TaxiCaller API");
            }
            
            JSONObject json = new JSONObject(response);
            return json.getString("token");
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching TaxiCaller token", e);
            throw new RuntimeException("Failed to authenticate with TaxiCaller: " + e.getMessage(), e);
        }
    }
    
    private String readErrorResponse(HttpURLConnection conn) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return "Unable to read error response";
        }
    }

    /**
     * Ensure we have a valid token for the current tenant
     */
    private String ensureToken() {
        String tenantKey = tenantConfigService.getTaxicallerApiKey();
        return tokenCache.computeIfAbsent(tenantKey, k -> fetchToken());
    }

    /**
     * Refresh token for current tenant
     */
    private String refreshToken() {
        String tenantKey = tenantConfigService.getTaxicallerApiKey();
        String newToken = fetchToken();
        if (newToken != null) {
            tokenCache.put(tenantKey, newToken);
        }
        return newToken;
    }

    /**
     * Fetch generic data from any TaxiCaller endpoint
     */
    public String fetchData(String endpoint) {
        try {
            String token = ensureToken();
            String urlString = getBaseUrl() + "/api/v1/company/" + getCompanyId() + "/" + endpoint;
            HttpURLConnection conn = createConnection(urlString, "GET", token);

            int responseCode = conn.getResponseCode();
            if (responseCode == 401) {
                log.info("Token expired. Fetching new token...");
                token = refreshToken();
                return fetchData(endpoint);
            }

            return readResponse(conn);
        } catch (TenantConfigurationException e) {
            throw e; // Let configuration exceptions propagate to GlobalExceptionHandler
        } catch (Exception e) {
            log.error("Error fetching data from TaxiCaller", e);
            return null;
        }
    }

    /**
     * Generate account job reports for a date range
     */
    public JSONArray generateAccountJobReports(LocalDate startDate, LocalDate endDate) {
        try {
            String token = refreshToken(); // Fresh token for each request
            String urlString = getBaseUrl() + "/api/v1/reports/typed/generate";
            HttpURLConnection conn = createConnection(urlString, "POST", token);

            JSONObject requestBody = new JSONObject();
            requestBody.put("company_id", getCompanyId());
            requestBody.put("report_type", "JOB");
            requestBody.put("output_format", "json");
            requestBody.put("template_id", 13908);

            // Constructing search_query
            JSONObject searchQuery = new JSONObject();
            
            // Adding custom period with date range
            JSONObject period = new JSONObject();
            period.put("@type", "custom");
            period.put("start", startDate.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            period.put("end", endDate.atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            searchQuery.put("period", period);

            // Adding filters
            JSONObject filters = new JSONObject();
            filters.put("0", new JSONObject().put("@type", "disabled"));
            filters.put("12dd259a-95af-407e-987c-6f1cbe860182", new JSONObject().put("@type", "disabled"));
            filters.put("63be082f-be18-43f0-8456-aaf120666855", new JSONObject().put("@type", "disabled"));
            searchQuery.put("filters", filters);
            
            requestBody.put("search_query", searchQuery);

            writeRequestBody(conn, requestBody.toString());

            int responseCode = conn.getResponseCode();
            if (responseCode == 401) {
                System.err.println("Token expired during report generation");
                return null;
            }

            String response = readResponse(conn);
            JSONObject jsonObject = new JSONObject(response);
            JSONArray array = jsonObject.getJSONArray("rows");
            //System.out.println(array);
            return array;

        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate driver log on/off reports for a date range
     */
    public JSONArray generateDriverLogOnOffReports(LocalDate startDate, LocalDate endDate) {
        try {
            String token = refreshToken(); // Fresh token for each request
            String urlString = getBaseUrl() + "/api/v1/reports/typed/generate";
            HttpURLConnection conn = createConnection(urlString, "POST", token);

            JSONObject requestBody = new JSONObject();
            requestBody.put("company_id", getCompanyId());
            requestBody.put("report_type", "JOB");
            requestBody.put("output_format", "json");
            requestBody.put("template_id", 11118);

            // Constructing search_query
            JSONObject searchQuery = new JSONObject();
            
            // Adding custom period with date range
            JSONObject period = new JSONObject();
            period.put("@type", "custom");
            period.put("start", startDate.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            period.put("end", endDate.atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            searchQuery.put("period", period);

            // Adding filters
            JSONObject filters = new JSONObject();
            filters.put("0", new JSONObject().put("@type", "disabled"));
            filters.put("879341fa-1803-4997-805c-65b4366ad315", new JSONObject().put("@type", "disabled"));
            filters.put("c66832fe-900b-4cbb-9804-66408a8ba280", new JSONObject().put("@type", "disabled"));
            searchQuery.put("filters", filters);
            
            requestBody.put("search_query", searchQuery);

            writeRequestBody(conn, requestBody.toString());

            int responseCode = conn.getResponseCode();
            if (responseCode == 401) {
                System.err.println("Token expired during report generation");
                return null;
            }

            String response = readResponse(conn);
            JSONObject jsonObject = new JSONObject(response);
       JSONArray array = jsonObject.getJSONArray("rows");
            return array;

        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate driver job reports for a relative date range (e.g., last N days)
     */
    public JSONArray generateDriverJobReports(int daysCount, int offset) {
        try {
            String token = refreshToken(); // Fresh token for each request
            String urlString = getBaseUrl() + "/api/v1/reports/typed/generate";
            HttpURLConnection conn = createConnection(urlString, "POST", token);

            JSONObject requestBody = new JSONObject();
            requestBody.put("company_id", getCompanyId());
            requestBody.put("report_type", "JOB");
            requestBody.put("output_format", "json");
            requestBody.put("template_id", 14001);

            // Constructing search_query
            JSONObject searchQuery = new JSONObject();
            
            // Adding relative period
            JSONObject period = new JSONObject();
            period.put("@type", "relative");
            period.put("count", daysCount);
            period.put("offset", offset);
            period.put("unit", "day");
            searchQuery.put("period", period);

            // Adding filters
            JSONObject filters = new JSONObject();
            filters.put("0", new JSONObject().put("@type", "disabled"));
            searchQuery.put("filters", filters);
            
            requestBody.put("search_query", searchQuery);

            writeRequestBody(conn, requestBody.toString());

            int responseCode = conn.getResponseCode();
            if (responseCode == 401) {
                System.err.println("Token expired during report generation");
                return null;
            }

            String response = readResponse(conn);
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.getJSONArray("rows");

        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate driver job reports for a custom date range
     */
    public JSONArray generateDriverJobReports(LocalDate startDate, LocalDate endDate) {
        try {
            String token = refreshToken(); // Fresh token for each request
            String urlString = getBaseUrl() + "/api/v1/reports/typed/generate";
            HttpURLConnection conn = createConnection(urlString, "POST", token);

            JSONObject requestBody = new JSONObject();
            requestBody.put("company_id", getCompanyId());
            requestBody.put("report_type", "JOB");
            requestBody.put("output_format", "json");
            requestBody.put("template_id", 14001);

            // Constructing search_query
            JSONObject searchQuery = new JSONObject();

            // Adding custom period with date range
            JSONObject period = new JSONObject();
            period.put("@type", "custom");
            period.put("start", startDate.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            period.put("end", endDate.atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            searchQuery.put("period", period);

            // Adding filters
            JSONObject filters = new JSONObject();
            filters.put("0", new JSONObject().put("@type", "disabled"));
            searchQuery.put("filters", filters);

            requestBody.put("search_query", searchQuery);

            writeRequestBody(conn, requestBody.toString());

            int responseCode = conn.getResponseCode();
            if (responseCode == 401) {
                System.err.println("Token expired during report generation");
                return null;
            }

            String response = readResponse(conn);
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.getJSONArray("rows");

        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetch list of users from TaxiCaller
     */
    public String fetchUsers() {
        return fetchData("user/list");
    }

    /**
     * Fetch list of vehicles from TaxiCaller
     */
    public String fetchVehicles() {
        return fetchData("vehicle/list");
    }

    /**
     * Fetch report templates from TaxiCaller
     */
    public String fetchReportTemplates() {
        return fetchData("reports/templates");
    }

    // Helper method to create HTTP connection
    private HttpURLConnection createConnection(String urlString, String method, String token) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        return conn;
    }

    // Helper method to write JSON body
    private void writeRequestBody(HttpURLConnection conn, String body) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = body.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    }

    // Helper method to read HTTP response
    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
}