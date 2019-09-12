package com.gentics.mesh.plugin.mustread.config;

import org.apache.commons.lang.StringUtils;

public class ProjectConfig {

    private String name;
    private String folderName;
    private String mustReadGraphQlFile;
    private String dataGraphQlFile;
    private String uuidFieldName;

    public String getName() {
        return name;
    }

    public ProjectConfig setName(String name) {
        this.name = name;

        return this;
    }

    public boolean isConfigurationForProject(String name) {
        return (this.name.equals(name));
    }

    public String getFolderName() {
        return folderName;
    }

    public ProjectConfig setFolderName(String folderName) {
        this.folderName = folderName;

        return this;
    }

    public String getMustReadGraphQlFile() {
        return mustReadGraphQlFile;
    }

    public ProjectConfig setMustReadGraphQlFile(String mustReadGraphQlFile) {
        this.mustReadGraphQlFile = mustReadGraphQlFile;

        return this;
    }

    public String getDataGraphQlFile() {
        return dataGraphQlFile;
    }

    public ProjectConfig setDataGraphQlFile(String dataGraphQlFile) {
        this.dataGraphQlFile = dataGraphQlFile;

        return this;
    }

    public String getUuidFieldName() {
        if (StringUtils.isBlank(uuidFieldName)) {
            return "uuid";
        }

        return uuidFieldName;
    }

    public ProjectConfig setUuidFieldName(String uuidFieldName) {
        this.uuidFieldName = uuidFieldName;

        return this;
    }
}
