package com.taxi.domain.plugin.repository;

import com.taxi.domain.plugin.model.PluginExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PluginExecution entities.
 */
@Repository
public interface PluginExecutionRepository extends JpaRepository<PluginExecution, Long> {

    /**
     * Find executions by plugin ID, ordered by start time descending.
     *
     * @param pluginId Plugin identifier
     * @param pageable Pagination parameters
     * @return List of plugin executions
     */
    List<PluginExecution> findByPluginIdOrderByStartTimeDesc(String pluginId, Pageable pageable);

    /**
     * Find executions by plugin ID and status.
     *
     * @param pluginId Plugin identifier
     * @param status Execution status
     * @return List of plugin executions
     */
    List<PluginExecution> findByPluginIdAndStatus(String pluginId, String status);
}
