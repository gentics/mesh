package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.assertj.AbstractMeshAssert;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;

public class TagFamilyResponseAssert extends AbstractMeshAssert<TagFamilyResponseAssert, TagFamilyResponse> {

	public TagFamilyResponseAssert(TagFamilyResponse actual) {
		super(actual, TagFamilyResponseAssert.class);
	}

	public TagFamilyResponseAssert matches(TagFamily tagFamily) {
		assertGenericNode(tagFamily, actual);
		assertNotNull("Name field was not set in the rest response.", actual.getName());
		return this;
	}
}
