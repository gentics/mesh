package com.gentics.mesh.example;

import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;

public class TagFamilyExamples extends AbstractExamples {

	public TagFamilyResponse getTagFamilyResponse() {
		TagFamilyResponse response = new TagFamilyResponse();
		response.setPermissions("READ", "CREATE", "UPDATE");
		response.setName("Colors");
		response.setEdited(getTimestamp());
		response.setEditor(getUserReference());
		response.setCreated(getTimestamp());
		response.setCreator(getUserReference());
		return response;
	}

	public TagFamilyListResponse getTagFamilyListResponse() {
		TagFamilyListResponse tagFamilyListResponse = new TagFamilyListResponse();
		tagFamilyListResponse.getData().add(getTagFamilyResponse());
		setPaging(tagFamilyListResponse, 1, 10, 2, 20);
		return tagFamilyListResponse;
	}

	public TagFamilyUpdateRequest getTagFamilyUpdateRequest() {
		TagFamilyUpdateRequest updateRequest = new TagFamilyUpdateRequest();
		updateRequest.setName("Nicer colors");
		return updateRequest;
	}

	public TagFamilyCreateRequest getTagFamilyCreateRequest() {
		TagFamilyCreateRequest createRequest = new TagFamilyCreateRequest();
		createRequest.setName("Colors");
		return createRequest;
	}

}
