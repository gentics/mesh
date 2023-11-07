package com.gentics.mesh.core.rest.node;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;
import com.gentics.mesh.core.rest.tag.TagReference;

/**
 * POJO for a node update request.
 */
public class NodeUpdateRequest implements FieldContainer {

	@JsonProperty(required = false)
	@JsonPropertyDescription("ISO 639-1 language tag of the node content.")
	private String language;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Dynamic map with fields of the node content.")
	private FieldMap fields = new FieldMapImpl();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Version number which can be provided in order to handle and detect concurrent changes to the node content.")
	private String version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of tags that should be used to tag the node.")
	private List<TagReference> tags;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Whether the publish the node after updating.")
	private boolean publish = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Permissions to be granted to roles on the updated node.")
	private ObjectPermissionGrantRequest grant;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Image manipulation changes request.")
	private ImageManipulationRequest manipulation;

	public NodeUpdateRequest() {
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
	 */
	public NodeUpdateRequest setLanguage(String language) {
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
	 * @return this
	 */
	public NodeUpdateRequest setFields(FieldMap fields) {
		this.fields = fields;
		return this;
	}

	/**
	 * Get the version of the fields
	 * 
	 * @return version number
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the version of the fields
	 * 
	 * @param version
	 */
	public NodeUpdateRequest setVersion(String version) {
		this.version = version;
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
	public NodeUpdateRequest setTags(List<TagReference> tags) {
		this.tags = tags;
		return this;
	}

	/**
	 * Whether the created node shall be published
	 * @return true to publish
	 */
	public boolean isPublish() {
		return publish;
	}

	/**
	 * Set the publish flag
	 * @param publish flag
	 * @return Fluent API
	 */
	public NodeUpdateRequest setPublish(boolean publish) {
		this.publish = publish;
		return this;
	}

	/**
	 * Get the request to grant role permissions
	 * @return optional request
	 */
	public ObjectPermissionGrantRequest getGrant() {
		return grant;
	}

	/**
	 * Set the request to grant role permissions
	 * @param grant optional request
	 * @return Fluent API
	 */
	public NodeUpdateRequest setGrant(ObjectPermissionGrantRequest grant) {
		this.grant = grant;
		return this;
	}

	/**
	 * Get the image manipulation request.
	 * 
	 * @return
	 */
	public ImageManipulationRequest getManipulation() {
		return manipulation;
	}

	/**
	 * Set the image manipulation.
	 * 
	 * @param manipulation
	 * @return 
	 */
	public NodeUpdateRequest setManipulation(ImageManipulationRequest manipulation) {
		this.manipulation = manipulation;
		return this;
	}
}
