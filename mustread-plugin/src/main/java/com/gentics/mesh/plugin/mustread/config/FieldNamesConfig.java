package com.gentics.mesh.plugin.mustread.config;

import org.apache.commons.lang.StringUtils;

public class FieldNamesConfig {
    private String uuid;
    private String published;

    public FieldNamesConfig FieldNamesConfig() {
        return this;
    }

    public String getUuid() {
        if (StringUtils.isBlank(uuid)) {
            return "uuid";
        }

        return uuid;
    }

    public FieldNamesConfig setUuid(String uuid) {
        this.uuid = uuid;

        return this;
    }

    public String getPublished() {
        if (StringUtils.isBlank(published)) {
            return "isPublished";
        }

        return published;
    }

    public FieldNamesConfig setPublished(String published) {
        this.published = published;

        return this;
    }
}
