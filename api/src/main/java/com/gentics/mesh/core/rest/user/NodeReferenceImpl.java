package com.gentics.mesh.core.rest.user;

import com.gentics.mesh.core.rest.schema.SchemaReference;

public class NodeReferenceImpl implements NodeReference {

	private String projectName;
	private String uuid;
	private String displayName;
	private SchemaReference schema;;

	@Override
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the node uuid.
	 * 
	 * @param uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Return the project name.
	 * 
	 * @return
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * Set the project name.
	 * 
	 * @param projectName
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/**
	 * Return the display name.
	 * 
	 * @return
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Set the display name.
	 * 
	 * @param displayName
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Return the schema reference.
	 * 
	 * @return
	 */
	public SchemaReference getSchema() {
		return schema;
	}

	/**
	 * Set the schema reference.
	 * 
	 * @param schema
	 */
	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

}
