package com.gentics.mesh.plugin.mustread.config;

public class ProjectConfig {

	private String name;
	private String folderName;
    private QueryFilesConfig queryFiles;
    private FieldNamesConfig fieldNames;
    private boolean publishedOnly;

	public String getName() {
		return name;
	}

	public ProjectConfig setName(String name) {
		this.name = name;

		return this;
	}

	public boolean isConfigurationForProject(String name) {
		return this.name.equals(name);
	}

	public String getFolderName() {
		return folderName;
	}

	public ProjectConfig setFolderName(String folderName) {
		this.folderName = folderName;

		return this;
	}

    public FieldNamesConfig getFieldNames() {
        return fieldNames;
    }

    public ProjectConfig setFieldNames(FieldNamesConfig fieldNames) {
        this.fieldNames = fieldNames;

		return this;
	}

    public QueryFilesConfig getQueryFiles() {
        return queryFiles;
    }

    public ProjectConfig setQueryFiles(QueryFilesConfig queryFiles) {
        this.queryFiles = queryFiles;

		return this;
	}

    public boolean isPublishedOnly() {

        return publishedOnly;
    }

    public ProjectConfig setPublishedOnly(boolean publishedOnly) {
        this.publishedOnly = publishedOnly;

		return this;
	}
}
