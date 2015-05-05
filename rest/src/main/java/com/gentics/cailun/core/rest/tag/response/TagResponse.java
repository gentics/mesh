package com.gentics.cailun.core.rest.tag.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractTaggableModel;
import com.gentics.cailun.core.rest.content.response.ContentResponse;

public class TagResponse extends AbstractTaggableModel {

	private List<ContentResponse> contents = new ArrayList<>();

	private List<TagResponse> childTags = new ArrayList<>();

	public TagResponse() {
	}

	public List<ContentResponse> getContents() {
		return contents;
	}

	public void setContents(List<ContentResponse> contents) {
		this.contents = contents;
	}

	public List<TagResponse> getChildTags() {
		return childTags;
	}

	public void setChildTags(List<TagResponse> childTags) {
		this.childTags = childTags;
	}

}
