package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.assertj.AbstractMeshAssert;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaResponse;

public class SchemaResponseAssert extends AbstractMeshAssert<SchemaResponseAssert, SchemaResponse> {

	public SchemaResponseAssert(SchemaResponse actual) {
		super(actual, SchemaResponseAssert.class);
	}

	public void matches(SchemaContainer schema) {
		// TODO make schemas extends generic nodes?
		// assertGenericNode(schema, restSchema);
		assertNotNull(schema);
		assertNotNull(actual);
		// assertEquals("Name does not match with the requested name.", schema.getName(), restSchema.getName());
		// assertEquals("Description does not match with the requested description.", schema.getDescription(), restSchema.getDescription());
		// assertEquals("Display names do not match.", schema.getDisplayName(), restSchema.getDisplayName());
		// TODO verify other fields
	}

}
