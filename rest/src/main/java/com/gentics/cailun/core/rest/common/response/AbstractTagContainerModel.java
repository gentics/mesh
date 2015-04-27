package com.gentics.cailun.core.rest.common.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.tag.response.TagResponse;

public abstract class AbstractTagContainerModel extends AbstractRestModel {
	private List<TagResponse> tags = new ArrayList<>();

	public List<TagResponse> getTags() {
		return tags;
	}

	public void setTags(List<TagResponse> tags) {
		this.tags = tags;
	}

}
