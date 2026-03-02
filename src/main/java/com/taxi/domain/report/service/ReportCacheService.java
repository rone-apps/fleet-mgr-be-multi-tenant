package com.taxi.domain.report.service;

import com.taxi.domain.report.ReportJobStatus;
import com.taxi.web.dto.report.DriverSummaryReportDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ReportCacheService {

    // jobId -> (pageNum -> cached page DTO)
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, DriverSummaryReportDTO>> pageCache = new ConcurrentHashMap<>();

    // jobId -> grand totals DTO
    private final ConcurrentHashMap<String, DriverSummaryReportDTO> grandTotalsCache = new ConcurrentHashMap<>();

    // jobId -> creation time (for expiry)
    private final ConcurrentHashMap<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();

    private static final long EXPIRY_MINUTES = 60;

    public void putPage(String jobId, int pageNum, DriverSummaryReportDTO dto) {
        pageCache.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>()).put(pageNum, dto);
        cacheTimestamps.putIfAbsent(jobId, LocalDateTime.now());
    }

    public DriverSummaryReportDTO getPage(String jobId, int pageNum) {
        ConcurrentHashMap<Integer, DriverSummaryReportDTO> pages = pageCache.get(jobId);
        return pages != null ? pages.get(pageNum) : null;
    }

    public void putGrandTotals(String jobId, DriverSummaryReportDTO dto) {
        grandTotalsCache.put(jobId, dto);
    }

    public DriverSummaryReportDTO getGrandTotals(String jobId) {
        return grandTotalsCache.get(jobId);
    }

    public void remove(String jobId) {
        pageCache.remove(jobId);
        grandTotalsCache.remove(jobId);
        cacheTimestamps.remove(jobId);
        ReportJobStatus.remove(jobId);
    }

    @Scheduled(fixedRate = 600000) // every 10 minutes
    public void cleanupExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRY_MINUTES);
        Iterator<Map.Entry<String, LocalDateTime>> it = cacheTimestamps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = it.next();
            if (entry.getValue().isBefore(cutoff)) {
                String jobId = entry.getKey();
                log.info("Cleaning up expired report cache for job: {}", jobId);
                pageCache.remove(jobId);
                grandTotalsCache.remove(jobId);
                ReportJobStatus.remove(jobId);
                it.remove();
            }
        }
    }
}
