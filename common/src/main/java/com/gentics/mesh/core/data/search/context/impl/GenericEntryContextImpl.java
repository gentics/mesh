package com.gentics.mesh.core.data.search.context.impl;

import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * Index entry context which contains information about the scope of the action. The index handler implementation may use this information in order to correctly
 * identify the elements which need to be stored.
 */
public class GenericEntryContextImpl implements GenericEntryContext {

	private String projectUuid;
	private String branchUuid;
	private String elementUuid;
	private ContainerType containerType;
	private String languageTag;
	private String schemaContainerVersionUuid;
	private String oldContainerUuid;
	private String newContainerUuid;

	@Override
	public String getProjectUuid() {
		return projectUuid;
	}

	@Override
	public GenericEntryContext setProjectUuid(String projectUuid) {
		this.projectUuid = projectUuid;
		return this;
	}

	@Override
	public String getBranchUuid() {
		return branchUuid;
	}

	@Override
	public GenericEntryContext setBranchUuid(String branchUuid) {
		this.branchUuid = branchUuid;
		return this;
	}

	@Override
	public ContainerType getContainerType() {
		return containerType;
	}

	@Override
	public GenericEntryContextImpl setContainerType(ContainerType containerType) {
		this.containerType = containerType;
		return this;
	}

	@Override
	public String getLanguageTag() {
		return languageTag;
	}

	@Override
	public GenericEntryContextImpl setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
		return this;
	}

	@Override
	public String getSchemaContainerVersionUuid() {
		return schemaContainerVersionUuid;
	}

	@Override
	public GenericEntryContextImpl setSchemaContainerVersionUuid(String schemaContainerVersionUuid) {
		this.schemaContainerVersionUuid = schemaContainerVersionUuid;
		return this;
	}

	@Override
	public String toString() {
		return " type: " + getContainerType() + " branch: " + getBranchUuid() + " project: " + getProjectUuid() + " languageTag: " + languageTag
				+ " schemaContainerVersionUuid: " + schemaContainerVersionUuid;
	}

	/**
	 * Return the old container uuid.
	 * 
	 * @return Uuid
	 */
	public String getOldContainerUuid() {
		return oldContainerUuid;
	}

	/**
	 * Set the old container uuid.
	 * 
	 * @param uuid
	 * @return Fluent API
	 */
	public GenericEntryContextImpl setOldContainerUuid(String uuid) {
		this.oldContainerUuid = uuid;
		return this;
	}

	/**
	 * Return the new container uuid.
	 * 
	 * @return
	 */
	public String getNewContainerUuid() {
		return newContainerUuid;
	}

	/**
	 * Set the new container uuid.
	 * 
	 * @param newContainerUuid
	 * @return Fluent API
	 */
	public GenericEntryContextImpl setNewContainerUuid(String newContainerUuid) {
		this.newContainerUuid = newContainerUuid;
		return this;
	}

	public String getElementUuid() {
		return elementUuid;
	}

	public void setElementUuid(String elementUuid) {
		this.elementUuid = elementUuid;
	}
}
