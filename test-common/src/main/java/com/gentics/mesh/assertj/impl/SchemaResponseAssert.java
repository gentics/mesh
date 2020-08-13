package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gentics.mesh.assertj.AbstractMeshAssert;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

public class SchemaResponseAssert extends AbstractMeshAssert<SchemaResponseAssert, SchemaResponse> {

	public SchemaResponseAssert(SchemaResponse actual) {
		super(actual, SchemaResponseAssert.class);
	}

	public SchemaResponseAssert matches(SchemaModel schema) {
		assertNotNull(schema);
		assertNotNull(actual);
		assertEquals("The name of the schemas do not match.", actual.getName(), schema.getName());
		assertEquals("The description of the schemas do not match.", actual.getDescription(), schema.getDescription());
		assertEquals("The displayField of the schemas do not match.", actual.getDisplayField(), schema.getDisplayField());
		assertEquals("The segmentField of the schemas do not match.", actual.getSegmentField(), schema.getSegmentField());
		// TODO assert for schema properties
		return this;
	}

	public SchemaResponseAssert isValid() {
		actual.validate();
		return this;
	}

	public SchemaResponseAssert matches(Schema schema) {
		// TODO make schemas extends generic nodes?
		// assertGenericNode(schema, restSchema);
		assertNotNull(schema);
		assertNotNull(actual);

		String creatorUuid = schema.getCreator().getUuid();
		String editorUuid = schema.getEditor().getUuid();
		assertEquals("The editor of the schema did not match up.", editorUuid, actual.getEditor().getUuid());
		assertEquals("The creator of the schema did not match up.", creatorUuid, actual.getCreator().getUuid());
		assertEquals("The creation date did not match up", schema.getCreationDate(), actual.getCreated());
		assertEquals("The edited date did not match up", schema.getLastEditedDate(), actual.getEdited());
		// assertEquals("Name does not match with the requested name.", schema.getName(), restSchema.getName());
		// assertEquals("Description does not match with the requested description.", schema.getDescription(), restSchema.getDescription());
		// assertEquals("Display names do not match.", schema.getDisplayName(), restSchema.getDisplayName());
		// TODO verify other fields
		return this;
	}

	public SchemaResponseAssert matches(SchemaVersion version) {
		assertNotNull(version);
		assertNotNull(actual);

		SchemaModel storedSchema = version.getSchema();
		matches(storedSchema);
		Schema container = version.getSchemaContainer();
		matches(container);
		return this;
	}

	public SchemaResponseAssert autoPurgeIsEnabled() {
		assertTrue("We expected the schema auto purge flag to be set to true.", actual.getAutoPurge());
		return this;
	}

	public SchemaResponseAssert autoPurgeIsNotSet() {
		assertNull("We expected the schema auto purge flag not to be set.", actual.getAutoPurge());
		return this;
	}

	public SchemaResponseAssert isAutoPurgeDisabled() {
		assertFalse("We expected the schema auto purge flag to be set to false.", actual.getAutoPurge());
		return this;
	}

}
