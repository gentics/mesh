package com.gentics.mesh.core.data;

/**
 * Index entry context which contains information about the scope of the action. The index handler implementation may use this information in order to correctly
 * identify the elements which need to be stored.
 */
public class HandleContext {

	private String projectUuid;
	private String releaseUuid;
	private ContainerType containerType;
	private String languageTag;
	private String schemaContainerVersionUuid;

	/**
	 * Return the project uuid.
	 * 
	 * @return
	 */
	public String getProjectUuid() {
		return projectUuid;
	}

	/**
	 * Set the project uuid.
	 * 
	 * @param projectUuid
	 * @return Fluent API
	 */
	public HandleContext setProjectUuid(String projectUuid) {
		this.projectUuid = projectUuid;
		return this;
	}

	/**
	 * Set the release uuid.
	 * 
	 * @return
	 */
	public String getReleaseUuid() {
		return releaseUuid;
	}

	/**
	 * Set the release uuid.
	 * 
	 * @param releaseUuid
	 * @return Fluent API
	 */
	public HandleContext setReleaseUuid(String releaseUuid) {
		this.releaseUuid = releaseUuid;
		return this;
	}

	/**
	 * Return the container type.
	 * 
	 * @return
	 */
	public ContainerType getContainerType() {
		return containerType;
	}

	/**
	 * Set the container type.
	 * 
	 * @param containerType
	 * @return Fluent API
	 */
	public HandleContext setContainerType(ContainerType containerType) {
		this.containerType = containerType;
		return this;
	}

	/**
	 * Return the container language.
	 * 
	 * @return
	 */
	public String getLanguageTag() {
		return languageTag;
	}

	/**
	 * Set the container language.
	 * 
	 * @param languageTag
	 * @return Fluent API
	 */
	public HandleContext setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
		return this;
	}

	/**
	 * Return the schema container version uuid.
	 * 
	 * @return
	 */
	public String getSchemaContainerVersionUuid() {
		return schemaContainerVersionUuid;
	}

	/**
	 * Set the schema container version uuid.
	 * 
	 * @param schemaContainerVersionUuid
	 * @return Fluent API
	 */
	public HandleContext setSchemaContainerVersionUuid(String schemaContainerVersionUuid) {
		this.schemaContainerVersionUuid = schemaContainerVersionUuid;
		return this;
	}

	@Override
	public String toString() {
		return " type: " + getContainerType() + " release: " + getReleaseUuid() + " project: " + getProjectUuid() + " languageTag: " + languageTag
				+ " schemaContainerVersionUuid: " + schemaContainerVersionUuid;
	}
}
