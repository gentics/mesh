package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.parameter.NodeParameters;

/**
 * A node reference contains the bare minimum of useful information which identifies a node. Various field in the {@link NodeResponse} utilize these references
 * in order to reduce data.
 */
public class NodeReference implements ExpandableNode {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the project to which the node belongs")
	private String projectName;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the node")
	private String uuid;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional display name of the node. A display field must be set in the schema in order to populate this property.")
	private String displayName;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Webroot path of the node. The path property will only be provided if the "
			+ NodeParameters.RESOLVE_LINKS_QUERY_PARAM_KEY + " query parameter has been set.")
	private String path;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema of the node")
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
	public NodeReference setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	/**
	 * Return the webroot path to the node (if it has one)
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path to the node.
	 * 
	 * @param path
	 * @return Fluent API
	 */
	public NodeReference setPath(String path) {
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
	 * @return Fluent API
	 */
	public NodeReference setProjectName(String projectName) {
		this.projectName = projectName;
		return this;
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
	public NodeReference setDisplayName(String displayName) {
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
	public NodeReference setSchema(SchemaReference schema) {
		this.schema = schema;
		return this;
	}

}
