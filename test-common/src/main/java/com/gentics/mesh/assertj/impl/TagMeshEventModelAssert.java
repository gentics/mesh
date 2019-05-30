package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.assertj.AbstractMeshElementEventModelAssert;
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;

public class TagMeshEventModelAssert extends AbstractMeshElementEventModelAssert<TagMeshEventModelAssert, TagMeshEventModel> {

	public TagMeshEventModelAssert(TagMeshEventModel actual) {
		super(actual, TagMeshEventModelAssert.class);
	}

	public TagMeshEventModelAssert hasTagFamily(String tagFamilyName, String tagFamilyUuid) {
		assertNotNull("The tag family reference was not included in the tag event.", actual.getTagFamily());
		assertEquals("The tag family name did not match.", tagFamilyName, actual.getTagFamily().getName());
		assertEquals("The tag family uuid did not match.", tagFamilyUuid, actual.getTagFamily().getUuid());
		return this;
	}

	public TagMeshEventModelAssert hasProject(String projectName, String projectUuid) {
		assertNotNull("The project information was missing in the tag event", actual.getProject());
		assertEquals("Project name of the tag event did not match.", projectName, actual.getProject().getName());
		assertEquals("Project uuid of the tag event did not match.", projectUuid, actual.getProject().getUuid());
		return this;
	}

}
