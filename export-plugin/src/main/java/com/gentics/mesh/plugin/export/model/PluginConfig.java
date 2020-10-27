package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.List;

public class PluginConfig {
    public static final Long DEFAULT_CONFIG_CACHE_TIME = 600L;

    @JsonProperty(required = true)
    @JsonPropertyDescription("The webroot paths of the configuration nodes")
    private List<String> configPaths = new ArrayList<>();

    @JsonPropertyDescription("The cache time (seconds) for the config nodes, defaults to: `600` ")
    private Long configNodesCacheTime = DEFAULT_CONFIG_CACHE_TIME;

    public PluginConfig() {
        this.configPaths = new ArrayList<>();
    }

    public List<String> getConfigPaths() {
        return configPaths;
    }

    public void setConfigPaths(List<String> configPaths) {
        this.configPaths = configPaths;
    }

    public Long getConfigNodesCacheTime() {
        return configNodesCacheTime;
    }

    public Long getConfigNodesCacheTimeMs() {
        return configNodesCacheTime * 1000;
    }

    public void setConfigNodesCacheTime(Long configNodesCacheTime) {
        this.configNodesCacheTime = configNodesCacheTime;
    }
}
