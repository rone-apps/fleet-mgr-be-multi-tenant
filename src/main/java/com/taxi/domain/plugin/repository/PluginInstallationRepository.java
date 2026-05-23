package com.taxi.domain.plugin.repository;

import com.taxi.domain.plugin.model.PluginInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PluginInstallation entities.
 */
@Repository
public interface PluginInstallationRepository extends JpaRepository<PluginInstallation, Long> {

    /**
     * Find plugin installation by plugin ID.
     *
     * @param pluginId Plugin identifier
     * @return Plugin installation, or empty if not found
     */
    Optional<PluginInstallation> findByPluginId(String pluginId);

    /**
     * Find all active plugin installations.
     *
     * @param active Active status
     * @return List of active plugin installations
     */
    List<PluginInstallation> findByActive(boolean active);

    /**
     * Find active installation by plugin ID.
     *
     * @param pluginId Plugin identifier
     * @param active Active status
     * @return Plugin installation, or empty if not found
     */
    Optional<PluginInstallation> findByPluginIdAndActive(String pluginId, boolean active);
}
