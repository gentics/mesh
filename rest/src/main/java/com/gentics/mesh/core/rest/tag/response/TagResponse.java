package com.gentics.mesh.core.rest.tag.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.response.AbstractTaggableModel;
import com.gentics.mesh.core.rest.content.response.ContentResponse;

public class TagResponse extends AbstractTaggableModel {

	private List<ContentResponse> childContents = new ArrayList<>();

	private List<TagResponse> childTags = new ArrayList<>();

	public TagResponse() {
	}

	public List<ContentResponse> getContents() {
		return childContents;
	}

	public void setContents(List<ContentResponse> contents) {
		this.childContents = contents;
	}

	public List<TagResponse> getChildTags() {
		return childTags;
	}

	public void setChildTags(List<TagResponse> childTags) {
		this.childTags = childTags;
	}

}
