package com.gentics.mesh.util;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;

/**
 * Utils for core tests.
 * 
 * @author plyhun
 *
 */
public final class CoreTestUtils {

	private CoreTestUtils() {
	}

	public static HibNodeFieldContainer createContainer() {
		return createContainer(FieldUtil.createStringFieldSchema("dummy"));
	}
	
	public static HibNodeFieldContainer createContainer(FieldSchema field) {
		CommonTx ctx = CommonTx.get();
		// 1. Setup schema
		HibSchema schemaContainer = ctx.schemaDao().createPersisted(null);
		HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schemaContainer);
		version.setSchemaContainer(schemaContainer);

		SchemaVersionModel schema = createSchema(field);
		version.setSchema(schema);

		HibNodeFieldContainer container = ctx.contentDao().createPersisted(version, null);
		container.setSchemaContainerVersion(version);
		return container;
	}

	public static SchemaVersionModel createSchema(FieldSchema field) {
		SchemaVersionModel schema = new SchemaModelImpl();
		schema.setName("dummySchema");
		if (field != null) {
			schema.addField(field);
		}
		return schema;
	}
}
