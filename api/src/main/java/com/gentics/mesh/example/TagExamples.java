package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
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

	public TagResponse getTagResponse1() {
		TagResponse tag = new TagResponse();
		tag.setUuid(randomUUID());
		tag.setCreated(getTimestamp());
		tag.setCreator(getUserReference());
		tag.setEdited(getTimestamp());
		tag.setEditor(getUserReference());
		tag.getFields().setName("tagName");
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
		list.getData().add(getTagResponse1());
		list.getData().add(getTagResponse2());
		setPaging(list, 1, 10, 2, 20);
		return list;
	}

	public TagUpdateRequest getTagUpdateRequest() {
		TagUpdateRequest tagUpdate = new TagUpdateRequest();
		//TODO add values!
		return tagUpdate;
	}

	public TagCreateRequest getTagCreateRequest() {
		TagCreateRequest tagCreate = new TagCreateRequest();
		//tagCreate.setTagFamily(tagFamilyReference);
		//TODO add values!
		return tagCreate;

	}
}
