package com.gentics.mesh.core.rest.node;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.user.UserReference;

/**
 * POJO for the rest model of the publish status of a single language of a node
 */
public class PublishStatusModel implements RestModel {

	private boolean published;

	private VersionReference version;

	private UserReference publisher;

	private String publishDate;

	public PublishStatusModel() {
	}

	public boolean isPublished() {
		return published;
	}

	public PublishStatusModel setPublished(boolean published) {
		this.published = published;
		return this;
	}

	public VersionReference getVersion() {
		return version;
	}

	public PublishStatusModel setVersion(VersionReference version) {
		this.version = version;
		return this;
	}

	public UserReference getPublisher() {
		return publisher;
	}

	public PublishStatusModel setPublisher(UserReference publisher) {
		this.publisher = publisher;
		return this;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public PublishStatusModel setPublishTime(String publishDate) {
		this.publishDate = publishDate;
		return this;
	}
}
