package com.gentics.mesh.example;

import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;

public class TagFamilyExamples extends AbstractExamples {

	public TagFamilyResponse getTagFamilyResponse(String name) {
		TagFamilyResponse response = new TagFamilyResponse();
		response.setPermissions("READ", "CREATE", "UPDATE");
		response.setName(name);
		response.setEdited(getTimestamp());
		response.setEditor(getUserReference());
		response.setCreated(getTimestamp());
		response.setCreator(getUserReference());
		return response;
	}

	public TagFamilyListResponse getTagFamilyListResponse() {
		TagFamilyListResponse tagFamilyListResponse = new TagFamilyListResponse();
		tagFamilyListResponse.getData().add(getTagFamilyResponse("Colors"));
		setPaging(tagFamilyListResponse, 1, 10, 2, 20);
		return tagFamilyListResponse;
	}

	public TagFamilyUpdateRequest getTagFamilyUpdateRequest(String name) {
		TagFamilyUpdateRequest updateRequest = new TagFamilyUpdateRequest();
		updateRequest.setName(name);
		return updateRequest;
	}

	public TagFamilyCreateRequest getTagFamilyCreateRequest(String name) {
		TagFamilyCreateRequest createRequest = new TagFamilyCreateRequest();
		createRequest.setName(name);
		return createRequest;
	}

}
