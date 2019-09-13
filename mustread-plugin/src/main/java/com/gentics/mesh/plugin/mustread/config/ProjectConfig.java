package com.gentics.mesh.plugin.mustread.config;

import org.apache.commons.lang.StringUtils;

public class ProjectConfig {

	private String name;
	private String folderName;
	private String uuidQueryFile;
	private String dataQueryFile;
	private String uuidFieldName;

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

	public String getDataQueryFile() {
		return dataQueryFile;
	}

	public ProjectConfig setDataQueryFile(String dataQueryFile) {
		this.dataQueryFile = dataQueryFile;
		return this;
	}

	public String getUuidQueryFile() {
		return uuidQueryFile;
	}

	public ProjectConfig setUuidQueryFile(String uuidQueryFile) {
		this.uuidQueryFile = uuidQueryFile;
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
