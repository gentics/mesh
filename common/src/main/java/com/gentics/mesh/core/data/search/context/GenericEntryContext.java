package com.gentics.mesh.core.data.search.context;

import com.gentics.mesh.core.data.ContainerType;

public interface GenericEntryContext extends EntryContext {

	/**
	 * Return the project uuid.
	 * 
	 * @return
	 */
	String getProjectUuid();

	/**
	 * Set the project uuid.
	 * 
	 * @param projectUuid
	 * @return Fluent API
	 */
	GenericEntryContext setProjectUuid(String projectUuid);

	/**
	 * Set the release uuid.
	 * 
	 * @return
	 */
	String getReleaseUuid();

	/**
	 * Set the release uuid.
	 * 
	 * @param releaseUuid
	 * @return Fluent API
	 */
	GenericEntryContext setReleaseUuid(String releaseUuid);

	/**
	 * Return the container type.
	 * 
	 * @return
	 */
	ContainerType getContainerType();

	/**
	 * Set the container type.
	 * 
	 * @param containerType
	 * @return Fluent API
	 */
	GenericEntryContext setContainerType(ContainerType containerType);

	/**
	 * Return the container language.
	 * 
	 * @return
	 */
	String getLanguageTag();

	/**
	 * Set the container language.
	 * 
	 * @param languageTag
	 * @return Fluent API
	 */
	GenericEntryContext setLanguageTag(String languageTag);

	/**
	 * Return the schema container version uuid.
	 * 
	 * @return
	 */
	String getSchemaContainerVersionUuid();

}
