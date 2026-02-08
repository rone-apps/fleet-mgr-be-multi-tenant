package com.taxi.web.dto.expense;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.expense.service.CabMatchingService.MatchingCabsPreview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for matching cabs preview
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingCabsPreviewDTO {

    private int totalCount;

    private List<CabSummaryDTO> sampleCabs;

    private boolean hasMore;

    /**
     * Convert from service object to DTO
     */
    public static MatchingCabsPreviewDTO fromService(MatchingCabsPreview preview) {
        if (preview == null) {
            return new MatchingCabsPreviewDTO(0, null, false);
        }

        List<CabSummaryDTO> cabSummaries = preview.sampleCabs.stream()
            .map(CabSummaryDTO::fromEntity)
            .collect(Collectors.toList());

        return MatchingCabsPreviewDTO.builder()
            .totalCount(preview.totalCount)
            .sampleCabs(cabSummaries)
            .hasMore(preview.hasMore)
            .build();
    }

    /**
     * Simple cab summary for preview
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CabSummaryDTO {
        private Long id;
        private String cabNumber;
        private String shareType;
        private Boolean hasAirportLicense;
        private String cabShiftType;
        private String cabType;

        public static CabSummaryDTO fromEntity(Cab cab) {
            // Note: Attributes are now at shift level, not cab level
            return CabSummaryDTO.builder()
                .id(cab.getId())
                .cabNumber(cab.getCabNumber())
                .shareType(null)  // Attributes moved to shift level
                .hasAirportLicense(false)
                .cabShiftType(null)
                .cabType(null)
                .build();
        }
    }
}
