package com.gentics.mesh.core.rest.user;

import com.gentics.mesh.core.rest.common.NameUuidReference;
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
	 *            Uuid of the node
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Return the project name.
	 * 
	 * @return Name of the project to which the node belongs
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * Set the project name.
	 * 
	 * @param projectName
	 *            Name of the project to which the node belongs
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/**
	 * Return the display name.
	 * 
	 * @return Display name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Set the display name.
	 * 
	 * @param displayName
	 *            Display name
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Return the schema reference.
	 * 
	 * @return Schema reference of the node
	 */
	public SchemaReference getSchema() {
		return schema;
	}

	/**
	 * Set the schema reference.
	 * 
	 * @param schema
	 *            Schema reference for the node
	 */
	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

}
