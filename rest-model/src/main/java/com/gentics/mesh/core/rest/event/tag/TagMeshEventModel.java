package com.gentics.mesh.core.rest.event.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.AbstractProjectEventModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;

public class TagMeshEventModel extends AbstractProjectEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the tag family of the tag.")
	private TagFamilyReference tagFamily;

	@JsonCreator
	public TagMeshEventModel(String origin, EventCauseInfo cause, MeshEvent event, String uuid, String name, ProjectReference project,
		TagFamilyReference tagFamily) {
		super(origin, cause, event, uuid, name, project);
		this.tagFamily = tagFamily;
	}

	public TagFamilyReference getTagFamily() {
		return tagFamily;
	}

	public void setTagFamily(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
	}

}
