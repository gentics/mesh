package com.gentics.mesh.util;

import java.util.UUID;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import org.apache.commons.lang3.RandomUtils;

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
		SchemaVersionModel schema = createSchema(field);
		HibSchema schemaContainer = ctx.schemaDao().create(schema, null, null, false);
		HibSchemaVersion version = schemaContainer.getLatestVersion();
		version.setSchema(schema);

		HibNodeFieldContainer container = ctx.contentDao().createPersisted(UUID.randomUUID().toString(), version, null);
		container.setSchemaContainerVersion(version);
		return container;
	}

	public static SchemaVersionModel createSchema(FieldSchema field) {
		SchemaVersionModel schema = new SchemaModelImpl();
		schema.setName(new String(RandomUtils.nextBytes(20)));
		if (field != null) {
			schema.addField(field);
		}
		return schema;
	}
}
