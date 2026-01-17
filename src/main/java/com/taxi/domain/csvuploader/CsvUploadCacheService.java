package com.taxi.domain.csvuploader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for parsed CSV data.
 * Stores parsed records temporarily until user confirms import.
 * Auto-expires entries after 30 minutes.
 */
@Service
@Slf4j
public class CsvUploadCacheService {

    private final Map<String, CachedUpload<?>> cache = new ConcurrentHashMap<>();
    
    private static final int EXPIRY_MINUTES = 30;

    /**
     * Store parsed credit card transactions
     */
    public void storeCreditCardData(String sessionId, List<CreditCardTransactionUploadDTO> data, CsvUploadPreviewDTO preview) {
        cache.put(sessionId, new CachedUpload<>(data, preview, UploadType.CREDIT_CARD));
        log.info("Cached {} credit card records for session {}", data.size(), sessionId);
    }

    /**
     * Store parsed mileage records
     */
    public void storeMileageData(String sessionId, List<MileageUploadDTO> data, CsvUploadPreviewDTO preview) {
        cache.put(sessionId, new CachedUpload<>(data, preview, UploadType.MILEAGE));
        log.info("Cached {} mileage records for session {}", data.size(), sessionId);
    }

    /**
     * Store parsed airport trip records
     */
    public void storeAirportTripData(String sessionId, List<AirportTripUploadDTO> data, CsvUploadPreviewDTO preview) {
        cache.put(sessionId, new CachedUpload<>(data, preview, UploadType.AIRPORT_TRIPS));
        log.info("Cached {} airport trip records for session {}", data.size(), sessionId);
    }

    /**
     * Retrieve cached credit card data
     */
    @SuppressWarnings("unchecked")
    public List<CreditCardTransactionUploadDTO> getCreditCardData(String sessionId) {
        CachedUpload<?> cached = cache.get(sessionId);
        if (cached != null && cached.getType() == UploadType.CREDIT_CARD) {
            return (List<CreditCardTransactionUploadDTO>) cached.getData();
        }
        return null;
    }

    /**
     * Retrieve cached mileage data
     */
    @SuppressWarnings("unchecked")
    public List<MileageUploadDTO> getMileageData(String sessionId) {
        CachedUpload<?> cached = cache.get(sessionId);
        if (cached != null && cached.getType() == UploadType.MILEAGE) {
            return (List<MileageUploadDTO>) cached.getData();
        }
        return null;
    }

    /**
     * Retrieve cached airport trip data
     */
    @SuppressWarnings("unchecked")
    public List<AirportTripUploadDTO> getAirportTripData(String sessionId) {
        CachedUpload<?> cached = cache.get(sessionId);
        if (cached != null && cached.getType() == UploadType.AIRPORT_TRIPS) {
            return (List<AirportTripUploadDTO>) cached.getData();
        }
        return null;
    }

    /**
     * Get preview metadata for a session
     */
    public CsvUploadPreviewDTO getPreview(String sessionId) {
        CachedUpload<?> cached = cache.get(sessionId);
        return cached != null ? cached.getPreview() : null;
    }

    /**
     * Check if session exists
     */
    public boolean hasSession(String sessionId) {
        return cache.containsKey(sessionId);
    }

    /**
     * Get upload type for session
     */
    public UploadType getUploadType(String sessionId) {
        CachedUpload<?> cached = cache.get(sessionId);
        return cached != null ? cached.getType() : null;
    }

    /**
     * Remove cached data after import
     */
    public void remove(String sessionId) {
        cache.remove(sessionId);
        log.info("Removed cached data for session {}", sessionId);
    }

    /**
     * Clean up expired entries every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRY_MINUTES);
        int removed = 0;
        
        for (Map.Entry<String, CachedUpload<?>> entry : cache.entrySet()) {
            if (entry.getValue().getCreatedAt().isBefore(cutoff)) {
                cache.remove(entry.getKey());
                removed++;
            }
        }
        
        if (removed > 0) {
            log.info("Cleaned up {} expired cache entries", removed);
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "totalSessions", cache.size(),
            "expiryMinutes", EXPIRY_MINUTES
        );
    }

    public enum UploadType {
        CREDIT_CARD,
        MILEAGE,
        AIRPORT_TRIPS
    }

    /**
     * Wrapper class for cached upload data
     */
    private static class CachedUpload<T> {
        private final List<T> data;
        private final CsvUploadPreviewDTO preview;
        private final UploadType type;
        private final LocalDateTime createdAt;

        public CachedUpload(List<T> data, CsvUploadPreviewDTO preview, UploadType type) {
            this.data = data;
            this.preview = preview;
            this.type = type;
            this.createdAt = LocalDateTime.now();
        }

        public List<T> getData() { return data; }
        public CsvUploadPreviewDTO getPreview() { return preview; }
        public UploadType getType() { return type; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
