package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.gentics.mesh.assertj.AbstractMeshAssert;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.rest.tag.TagResponse;

public class TagResponseAssert extends AbstractMeshAssert<TagResponseAssert, TagResponse> {

	public TagResponseAssert(TagResponse actual) {
		super(actual, TagResponseAssert.class);
	}

	public TagResponseAssert matches(HibTag tag) {
		assertGenericNode(tag, actual);
		assertEquals(tag.getUuid(), actual.getUuid());
		return this;
	}

	public TagResponseAssert hasName(String name) {
		assertThat(actual.getName()).as("Tag name").isEqualTo(name);
		return this;
	}

	public TagResponseAssert hasUuid(String uuid) {
		assertThat(actual.getUuid()).as("Tag uuid").isEqualTo(uuid);
		return this;
	}
}
