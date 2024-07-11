package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.assertj.AbstractMeshAssert;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.json.JsonUtil;

public class SchemaCreateRequestAssert extends AbstractMeshAssert<SchemaCreateRequestAssert, SchemaCreateRequest> {

	public SchemaCreateRequestAssert(SchemaCreateRequest actual) {
		super(actual, SchemaCreateRequestAssert.class);
	}

	public SchemaCreateRequestAssert matches(SchemaCreateRequest schema) {
		assertNotNull(schema);
		assertNotNull(actual);
		assertEquals("The name of the schemas do not match.", actual.getName(), schema.getName());
		assertEquals("The description of the schemas do not match.", actual.getDescription(), schema.getDescription());
		assertEquals("The displayField of the schemas do not match.", actual.getDisplayField(), schema.getDisplayField());
		assertEquals("The segmentField of the schemas do not match.", actual.getSegmentField(), schema.getSegmentField());
		// TODO assert for schema properties
		return this;
	}

	public SchemaCreateRequestAssert isValid() {
		actual.validate();
		return this;
	}

	public SchemaCreateRequestAssert matches(Schema schema) {
		// TODO make schemas extends generic nodes?
		// assertGenericNode(schema, restSchema);
		assertNotNull(schema);
		assertNotNull(actual);

		//		String creatorUuid = schema.getCreator().getUuid();
		//		String editorUuid = schema.getEditor().getUuid();
		// assertEquals("Name does not match with the requested name.", schema.getName(), restSchema.getName());
		// assertEquals("Description does not match with the requested description.", schema.getDescription(), restSchema.getDescription());
		// assertEquals("Display names do not match.", schema.getDisplayName(), restSchema.getDisplayName());
		// TODO verify other fields
		return this;
	}

	public SchemaCreateRequestAssert matches(SchemaVersion version) {
		assertNotNull(version);
		assertNotNull(actual);

		SchemaCreateRequest storedSchema = JsonUtil.readValue(version.getJson(), SchemaCreateRequest.class);
		matches(storedSchema);
		Schema container = version.getSchemaContainer();
		matches(container);
		return this;
	}

	public SchemaCreateRequestAssert matches(SchemaResponse response) {
		assertNotNull(response);
		assertNotNull(actual);
		assertEquals("The name of the schemas do not match.", actual.getName(), response.getName());
		assertEquals("The description of the schemas do not match.", actual.getDescription(), response.getDescription());
		assertEquals("The displayField of the schemas do not match.", actual.getDisplayField(), response.getDisplayField());
		assertEquals("The segmentField of the schemas do not match.", actual.getSegmentField(), response.getSegmentField());
		// TODO assert for schema properties
		return this;
	}

}
