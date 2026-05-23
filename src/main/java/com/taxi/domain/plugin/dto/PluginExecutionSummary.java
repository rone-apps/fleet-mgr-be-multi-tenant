package com.taxi.domain.plugin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary DTO for plugin execution history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluginExecutionSummary {

    private Long id;
    private String pluginId;
    private String executionType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private int recordsProcessed;
    private int recordsSuccess;
    private int recordsFailed;
    private String errorMessage;
}
