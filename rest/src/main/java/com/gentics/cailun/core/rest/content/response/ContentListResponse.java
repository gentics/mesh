package com.gentics.cailun.core.rest.content.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractListResponse;

public class ContentListResponse extends AbstractListResponse {

	//TODO make rest fields protected thoughout all classes
	private List<ContentResponse> contents = new ArrayList<>();

	public ContentListResponse() {

	}

	public List<ContentResponse> getContents() {
		return contents;
	}

}
