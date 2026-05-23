package com.taxi.infrastructure.plugin.registry;

import com.taxi.infrastructure.plugin.core.Plugin;
import com.taxi.infrastructure.plugin.core.PluginMetadata;
import com.taxi.infrastructure.plugin.core.PluginType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of PluginRegistry that auto-discovers plugins via Spring dependency injection.
 * All Spring beans implementing the Plugin interface are automatically registered.
 */
@Service
@Slf4j
public class PluginRegistryImpl implements PluginRegistry {

    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();

    /**
     * Constructor with auto-discovery of all Plugin beans.
     * Spring will inject all beans implementing the Plugin interface.
     *
     * @param autoDetectedPlugins List of all Plugin beans found by Spring
     */
    @Autowired
    public PluginRegistryImpl(List<Plugin> autoDetectedPlugins) {
        log.info("Initializing plugin registry with {} auto-detected plugins", autoDetectedPlugins.size());

        autoDetectedPlugins.forEach(plugin -> {
            try {
                register(plugin);
            } catch (Exception e) {
                log.error("Failed to register plugin: {}", plugin.getClass().getName(), e);
            }
        });

        log.info("Plugin registry initialized with {} plugins: {}",
                 plugins.size(),
                 plugins.keySet());
    }

    @Override
    public void register(Plugin plugin) {
        PluginMetadata metadata = plugin.getMetadata();
        String pluginId = metadata.getPluginId();

        if (plugins.containsKey(pluginId)) {
            log.warn("Plugin with ID '{}' is already registered. Overwriting with {}",
                     pluginId, plugin.getClass().getName());
        }

        plugins.put(pluginId, plugin);
        log.info("Registered plugin: {} ({})", metadata.getDisplayName(), pluginId);
    }

    @Override
    public Optional<Plugin> getPlugin(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }

    @Override
    public List<Plugin> getPluginsByType(PluginType type) {
        return plugins.values().stream()
                .filter(plugin -> plugin.getMetadata().getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public List<Plugin> getEnabledPlugins(String tenantId) {
        return plugins.values().stream()
                .filter(plugin -> plugin.isEnabled(tenantId))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, PluginMetadata> getAllPluginMetadata() {
        return plugins.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getMetadata()
                ));
    }

    @Override
    public List<Plugin> getAllPlugins() {
        return new ArrayList<>(plugins.values());
    }
}
