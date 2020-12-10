package com.gentics.mesh.core.data.search.context;

import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * Index entry context which contains information about the scope of the action. The index handler implementation may use this information in order to correctly
 * identify the elements which need to be stored.
 */
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
	 * Set the branch uuid.
	 * 
	 * @return
	 */
	String getBranchUuid();

	/**
	 * Set the branch uuid.
	 * 
	 * @param branchUuid
	 * @return Fluent API
	 */
	GenericEntryContext setBranchUuid(String branchUuid);

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

	/**
	 * Set the schema container version uuid.
	 * 
	 * @param uuid
	 * @return Fluent API
	 */
	GenericEntryContext setSchemaContainerVersionUuid(String uuid);

}
