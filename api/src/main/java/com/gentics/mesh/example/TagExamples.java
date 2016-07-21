package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFieldContainer;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;

public class TagExamples extends AbstractExamples {

	public TagFamilyReference getTagFamilyReference() {
		TagFamilyReference tagFamilyReference = new TagFamilyReference();
		tagFamilyReference.setName("colors");
		tagFamilyReference.setUuid(randomUUID());
		return tagFamilyReference;
	}

	public TagResponse getTagResponse1(String name) {
		TagResponse tag = new TagResponse();
		tag.setUuid(randomUUID());
		tag.setCreated(getTimestamp());
		tag.setCreator(getUserReference());
		tag.setEdited(getTimestamp());
		tag.setEditor(getUserReference());
		tag.getFields().setName(name);
		tag.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		tag.setTagFamily(getTagFamilyReference());
		return tag;
	}

	public TagResponse getTagResponse2() {
		TagResponse tag = new TagResponse();
		tag.setUuid(randomUUID());
		tag.setCreated(getTimestamp());
		tag.setCreator(getUserReference());
		tag.setEdited(getTimestamp());
		tag.setEditor(getUserReference());
		tag.getFields().setName("Name for language tag en");
		tag.setTagFamily(getTagFamilyReference());
		tag.setPermissions("READ", "CREATE");
		return tag;
	}

	public TagListResponse getTagListResponse() {
		TagListResponse list = new TagListResponse();
		list.getData().add(getTagResponse1("green"));
		list.getData().add(getTagResponse2());
		setPaging(list, 1, 10, 2, 20);
		return list;
	}

	public TagUpdateRequest getTagUpdateRequest(String name) {
		TagUpdateRequest request = new TagUpdateRequest();
		request.setFields(new TagFieldContainer().setName(name));
		return request;
	}

	public TagCreateRequest getTagCreateRequest(String name) {
		TagCreateRequest request = new TagCreateRequest();
		//tagCreate.setTagFamily(tagFamilyReference);
		request.setFields(new TagFieldContainer().setName(name));
		return request;
	}
}
