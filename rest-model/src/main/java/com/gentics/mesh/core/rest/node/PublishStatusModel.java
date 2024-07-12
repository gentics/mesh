package com.gentics.mesh.core.rest.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.user.UserReference;

/**
 * POJO for the rest model of the publish status of a single language of a node.
 */
public class PublishStatusModel implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the content is published.")
	private boolean published;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version number.")
	private String version;

	@JsonPropertyDescription("Reference to the publisher user of the node content.")
	private UserReference publisher;

	@JsonPropertyDescription("ISO8601 formatted publish date string.")
	private String publishDate;

	public PublishStatusModel() {

	}

	/**
	 * Return the flag which indicates that this version is published.
	 * 
	 * @return
	 */
	public boolean isPublished() {
		return published;
	}

	/**
	 * Set the flag which indicates that this version is published.
	 * 
	 * @param published
	 * @return
	 */
	public PublishStatusModel setPublished(boolean published) {
		this.published = published;
		return this;
	}

	/**
	 * Return the node version.
	 * 
	 * @return
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the version of the published node.
	 * 
	 * @param version
	 * @return Fluent API
	 */
	public PublishStatusModel setVersion(String version) {
		this.version = version;
		return this;
	}

	/**
	 * Return the publisher user reference.
	 * 
	 * @return
	 */
	public UserReference getPublisher() {
		return publisher;
	}

	/**
	 * Set the publisher user reference.
	 * 
	 * @param publisher
	 * @return Fluent API
	 */
	public PublishStatusModel setPublisher(UserReference publisher) {
		this.publisher = publisher;
		return this;
	}

	/**
	 * Return the publishing date.
	 * 
	 * @return
	 */
	public String getPublishDate() {
		return publishDate;
	}

	/**
	 * Set the publishing date.
	 * 
	 * @param publishDate
	 * @return Fluent API
	 */
	public PublishStatusModel setPublishDate(String publishDate) {
		this.publishDate = publishDate;
		return this;
	}
}
