package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.assertj.AbstractMeshElementEventModelAssert;
import com.gentics.mesh.core.rest.event.tagfamily.TagFamilyMeshEventModel;

public class TagFamilyMeshEventModelAssert extends AbstractMeshElementEventModelAssert<TagFamilyMeshEventModelAssert, TagFamilyMeshEventModel> {

	public TagFamilyMeshEventModelAssert(TagFamilyMeshEventModel actual) {
		super(actual, TagFamilyMeshEventModelAssert.class);
	}

	public TagFamilyMeshEventModelAssert hasProject(String projectName, String projectUuid) {
		assertNotNull("The project reference was missing in the tag family event.", actual.getProject());
		assertEquals("The project name was missing in the tag family event.", projectName, actual.getProject().getName());
		assertEquals("The project uuid was missing in the tag family event.", projectUuid, actual.getProject().getUuid());
		return this;
	}

}
