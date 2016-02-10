package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gentics.mesh.assertj.AbstractMeshAssert;
import com.gentics.mesh.core.rest.schema.Schema;

public class SchemaAssert extends AbstractMeshAssert<SchemaAssert, Schema> {

	public SchemaAssert(Schema actual) {
		super(actual, SchemaAssert.class);
	}



}
