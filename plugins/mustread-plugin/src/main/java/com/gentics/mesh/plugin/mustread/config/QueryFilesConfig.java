package com.gentics.mesh.plugin.mustread.config;

public class QueryFilesConfig {

    private String uuid;
    private String data;

    public QueryFilesConfig QueryFileConfig() {
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public QueryFilesConfig setUuid(String uuid) {
        this.uuid = uuid;

        return this;
    }

    public String getData() {
        return data;
    }

    public QueryFilesConfig setData(String data) {
        this.data = data;

        return this;
    }
}
