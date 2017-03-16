package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import com.gentics.mesh.assertj.AbstractMeshAssert;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.rest.tag.TagResponse;

public class TagResponseAssert extends AbstractMeshAssert<TagResponseAssert, TagResponse> {

	public TagResponseAssert(TagResponse actual) {
		super(actual, TagResponseAssert.class);
	}

	public TagResponseAssert matches(Tag tag) {
		assertGenericNode(tag, actual);
		// tag.setSchema(neo4jTemplate.fetch(tag.getSchema()));
		assertEquals(tag.getUuid(), actual.getUuid());
		// assertEquals(tag.getSchema().getUuid(), restTag.getSchema().getUuid());
		// assertEquals(tag.getSchema().getName(), restTag.getSchema().getSchemaName());
		return this;
	}
}
