package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.example.ExampleUuids.TAGFAMILY_COLORS_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAGFAMILY_FUELS_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAG_GREEN_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAG_RED_UUID;

import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;

public class TagExamples extends AbstractExamples {

	public TagFamilyReference getTagFamilyReference() {
		TagFamilyReference tagFamilyReference = new TagFamilyReference();
		tagFamilyReference.setName("colors");
		tagFamilyReference.setUuid(TAGFAMILY_COLORS_UUID);
		return tagFamilyReference;
	}

	public TagResponse createTagResponse1(String name) {
		TagResponse tag = new TagResponse();
		tag.setUuid(TAG_GREEN_UUID);
		tag.setCreated(createOldTimestamp());
		tag.setCreator(createUserReference());
		tag.setEdited(createNewTimestamp());
		tag.setEditor(createUserReference());
		tag.setName(name);
		tag.setPermissions(READ, UPDATE, DELETE, CREATE);
		tag.setTagFamily(getTagFamilyReference());
		return tag;
	}

	public TagResponse getTagResponse2() {
		TagResponse tag = new TagResponse();
		tag.setUuid(TAG_RED_UUID);
		tag.setCreated(createOldTimestamp());
		tag.setCreator(createUserReference());
		tag.setEdited(createNewTimestamp());
		tag.setEditor(createUserReference());
		tag.setName("Name for language tag en");
		tag.setTagFamily(getTagFamilyReference());
		tag.setPermissions(READ, CREATE);
		return tag;
	}

	public TagListResponse createTagListResponse() {
		TagListResponse list = new TagListResponse();
		list.getData().add(createTagResponse1("green"));
		list.getData().add(getTagResponse2());
		setPaging(list, 1, 10, 2, 20);
		return list;
	}

	public TagUpdateRequest createTagUpdateRequest(String name) {
		TagUpdateRequest request = new TagUpdateRequest();
		request.setName(name);
		return request;
	}

	public TagCreateRequest createTagCreateRequest(String name) {
		TagCreateRequest request = new TagCreateRequest();
		//tagCreate.setTagFamily(tagFamilyReference);
		request.setName(name);
		return request;
	}

	public TagReference createTagReference(String name, String tagFamilyName) {
		TagReference reference = new TagReference();
		reference.setName(name);
		reference.setTagFamily(tagFamilyName);
		reference.setUuid(TAGFAMILY_FUELS_UUID);
		return reference;
	}

	public TagListUpdateRequest getTagListUpdateRequest() {
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(createTagReference("green", "colors"));
		request.getTags().add(createTagReference("yellow", "colors"));
		return request;
	}
}
