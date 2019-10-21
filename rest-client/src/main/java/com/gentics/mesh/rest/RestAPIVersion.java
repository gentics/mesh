package com.gentics.mesh.rest;

public enum RestAPIVersion {
    V1("/api/v1"),
    V2("/api/v2");

    private String basePath;

    RestAPIVersion(String path) {
        this.basePath = path;
    }

    public String getBasePath() {
        return this.basePath;
    }
}
