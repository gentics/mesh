package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertTrue;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.node.FieldMap;

public class FieldMapAssert extends AbstractAssert<FieldMapAssert, FieldMap> {

	public FieldMapAssert(FieldMap actual) {
		super(actual, FieldMapAssert.class);
	}

	public void isEmpty() {
		assertTrue("We expect the fieldmap to be empty but it was not", actual.isEmpty());
	}

	public void isNotEmpty() {
		assertTrue("We expect the fieldmap to be not empty but it was", !actual.isEmpty());
	}

}
