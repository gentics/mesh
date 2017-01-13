package com.gentics.mesh.core.data;

public class HandleContext {

	private String projectUuid;
	private String releaseUuid;
	private ContainerType containerType;
	private String languageTag;
	private String schemaContainerVersionUuid;

	public String getProjectUuid() {
		return projectUuid;
	}

	public HandleContext setProjectUuid(String projectUuid) {
		this.projectUuid = projectUuid;
		return this;
	}

	public String getReleaseUuid() {
		return releaseUuid;
	}

	public HandleContext setReleaseUuid(String releaseUuid) {
		this.releaseUuid = releaseUuid;
		return this;
	}

	public ContainerType getContainerType() {
		return containerType;
	}

	public HandleContext setContainerType(ContainerType containerType) {
		this.containerType = containerType;
		return this;
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public HandleContext setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
		return this;
	}

	public String getSchemaContainerVersionUuid() {
		return schemaContainerVersionUuid;
	}

	public HandleContext setSchemaContainerVersionUuid(String schemaContainerVersionUuid) {
		this.schemaContainerVersionUuid = schemaContainerVersionUuid;
		return this;
	}

	@Override
	public String toString() {
		return  " type: " + getContainerType() + " release: " + getReleaseUuid() + " project: "
				+ getProjectUuid() + " languageTag: " + languageTag + " schemaContainerVersionUuid: " + schemaContainerVersionUuid;
	}
}
