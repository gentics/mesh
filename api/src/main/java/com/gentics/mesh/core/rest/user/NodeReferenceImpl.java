package com.gentics.mesh.core.rest.user;

import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * @see NodeReference
 */
public class NodeReferenceImpl implements NodeReference {

	private String projectName;
	private String uuid;
	private String displayName;
	private String path;
	private SchemaReference schema;

	@Override
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the node uuid.
	 * 
	 * @param uuid
	 *            Uuid of the node
	 * @return Fluent API
	 */
	public NodeReferenceImpl setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	public String getPath() {
		return path;
	}

	/**
	 * Set the path to the node.
	 * 
	 * @param path
	 * @return Fluent API
	 */
	public NodeReferenceImpl setPath(String path) {
		this.path = path;
		return this;
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
	 * @return Fluent API
	 */
	public NodeReferenceImpl setDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
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
	 * @return Fluent API
	 */
	public NodeReferenceImpl setSchema(SchemaReference schema) {
		this.schema = schema;
		return this;
	}

}
