package com.gentics.mesh.event.node;

import com.gentics.mesh.event.AbstractMeshEventModel;

public abstract class AbstractNodeMeshEventModel extends AbstractMeshEventModel {

	private String type;

	private String branchUuid;

	private String languageTag;

	private String schemaName;

	private String schemaUuid;

	/**
	 * Type of the node that has been deleted (e.g. published or draft)
	 * 
	 * @return
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getBranchUuid() {
		return branchUuid;
	}

	public void setBranchUuid(String branchUuid) {
		this.branchUuid = branchUuid;
	}

	/**
	 * Return the specific language tag that has been deleted from the node.
	 * 
	 * @return
	 */
	public String getLanguageTag() {
		return languageTag;
	}

	public void setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
	}

	public String getSchemaUuid() {
		return schemaUuid;
	}

	public void setSchemaUuid(String uuid) {
		this.schemaUuid = uuid;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String name) {
		schemaName = name;
	}

}
