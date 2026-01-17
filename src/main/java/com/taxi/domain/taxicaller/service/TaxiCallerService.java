package com.taxi.domain.taxicaller.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class TaxiCallerService {
    private static final String API_KEY = "5658f7a37c72163a41855005b23add80";
    private static final int COMPANY_ID = 42259;
    private static final String BASE_URL = "https://api.taxicaller.net";
    private String token = null;

    /**
     * Fetch a new JWT token from TaxiCaller API
     */
    private String fetchToken() {
        try {
            String urlString = BASE_URL + "/AdminService/v1/jwt/for-key?key=" + API_KEY + "&sub=*&ttl=900";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Failed to get token. HTTP Response: " + responseCode);
                return null;
            }

            String response = readResponse(conn);
            JSONObject json = new JSONObject(response);
            return json.getString("token");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ensure we have a valid token
     */
    private void ensureToken() {
        if (token == null) {
            token = fetchToken();
        }
    }

    /**
     * Fetch generic data from any TaxiCaller endpoint
     */
    public String fetchData(String endpoint) {
        try {
            ensureToken();
            String urlString = BASE_URL + "/api/v1/company/" + COMPANY_ID + "/" + endpoint;
            HttpURLConnection conn = createConnection(urlString, "GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 401) {
                System.out.println("Token expired. Fetching new token...");
                token = fetchToken();
                return fetchData(endpoint);
            }

            return readResponse(conn);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate account job reports for a date range
     */
    public JSONArray generateAccountJobReports(LocalDate startDate, LocalDate endDate) {
        try {
            token = fetchToken(); // Fresh token for each request
            String urlString = BASE_URL + "/api/v1/reports/typed/generate";
            HttpURLConnection conn = createConnection(urlString, "POST");

            JSONObject requestBody = new JSONObject();
            requestBody.put("company_id", COMPANY_ID);
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
            System.out.println(array);
            return array;

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
            token = fetchToken(); // Fresh token for each request
            String urlString = BASE_URL + "/api/v1/reports/typed/generate";
            HttpURLConnection conn = createConnection(urlString, "POST");

            JSONObject requestBody = new JSONObject();
            requestBody.put("company_id", COMPANY_ID);
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
            token = fetchToken(); // Fresh token for each request
            String urlString = BASE_URL + "/api/v1/reports/typed/generate";
            HttpURLConnection conn = createConnection(urlString, "POST");

            JSONObject requestBody = new JSONObject();
            requestBody.put("company_id", COMPANY_ID);
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
            token = fetchToken(); // Fresh token for each request
            String urlString = BASE_URL + "/api/v1/reports/typed/generate";
            HttpURLConnection conn = createConnection(urlString, "POST");

            JSONObject requestBody = new JSONObject();
            requestBody.put("company_id", COMPANY_ID);
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
    private HttpURLConnection createConnection(String urlString, String method) throws IOException {
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