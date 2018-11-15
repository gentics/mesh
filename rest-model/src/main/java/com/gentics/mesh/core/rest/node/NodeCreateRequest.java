package com.gentics.mesh.core.rest.node;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;

/**
 * POJO for a node create request.
 */
public class NodeCreateRequest implements FieldContainer {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the parent node in which the node will be created. The uuid of this object must be set.")
	private NodeReference parentNode;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema of the node.")
	@JsonDeserialize(as = SchemaReferenceImpl.class)
	private SchemaReference schema;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO 639-1 language tag of the node content.")
	private String language;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Dynamic map with fields of the node content.")
	private FieldMap fields = new FieldMapImpl();

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of tags that should be used to tag the node.")
	private List<TagReference> tags;

	public NodeCreateRequest() {
	}

	/**
	 * Return the schema name.
	 * 
	 * @return Schema reference
	 */
	public SchemaReference getSchema() {
		return schema;
	}

	/**
	 * Set the schema name.
	 * 
	 * @param schema Schema reference
	 * @return Fluent API
	 */
	public NodeCreateRequest setSchema(SchemaReference schema) {
		this.schema = schema;
		return this;
	}

	/**
	 * Return the parent node reference.
	 * 
	 * @return Node reference to parent node or null of the node has no parent node.
	 */
	public NodeReference getParentNode() {
		return parentNode;
	}

	/**
	 * Set the parent node reference for the node that should be created.
	 * 
	 * @param parentNode
	 * @return Fluent API
	 */
	public NodeCreateRequest setParentNode(NodeReference parentNode) {
		this.parentNode = parentNode;
		return this;
	}

	/**
	 * Helper method which can be used to quickly set the parent node uuid.
	 * 
	 * @param uuid
	 * @return Fluent API
	 */
	public NodeCreateRequest setParentNodeUuid(String uuid) {
		this.parentNode = new NodeReference().setUuid(uuid);
		return this;
	}

	/**
	 * Return the language of the node.
	 * 
	 * @return Language tag of the language for the node
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Set the language of the node which should be updated.
	 * 
	 * @param language Language tag for the node
	 * @return Fluent API
	 */
	public NodeCreateRequest setLanguage(String language) {
		this.language = language;
		return this;
	}

	/**
	 * Set the field which should be updated.
	 * 
	 * @return Fields of the node
	 */
	public FieldMap getFields() {
		return fields;
	}

	/**
	 * Set the fields which should be updated.
	 *
	 * @param fields A field map containing all fields to be updated.
	 * @return Fluent API
	 */
	public NodeCreateRequest setFields(FieldMap fields) {
		this.fields = fields;
		return this;
	}

	/**
	 * Shortcut to set the schema reference by schemaName.
	 * 
	 * @param schemaName
	 * @return
	 */
	public NodeCreateRequest setSchemaName(String schemaName) {
		SchemaReference schemaReference = new SchemaReferenceImpl();
		schemaReference.setName(schemaName);
		setSchema(schemaReference);
		return this;
	}

	/**
	 * Return the tag list.
	 * 
	 * @return
	 */
	public List<TagReference> getTags() {
		return tags;
	}

	/**
	 * Set the tag list.
	 * 
	 * @param tags
	 * @return Fluent API
	 */
	public NodeCreateRequest setTags(List<TagReference> tags) {
		this.tags = tags;
		return this;
	}

}
