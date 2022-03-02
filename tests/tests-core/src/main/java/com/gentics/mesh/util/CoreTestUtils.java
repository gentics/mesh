package com.gentics.mesh.util;

import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang.RandomStringUtils;

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

	public static HibNodeFieldContainer createContainer(FieldSchema... fields) {
		CommonTx ctx = CommonTx.get();
		// 1. Setup schema
		SchemaVersionModel schema = createSchema(fields);
		HibSchema schemaContainer = ctx.schemaDao().create(schema, null, null, false);
		HibSchemaVersion version = schemaContainer.getLatestVersion();
		ctx.commit();
		return ctx.contentDao().createPersisted(UUID.randomUUID().toString(), version, null);
	}

	public static SchemaVersionModel createSchema(FieldSchema... fields) {
		SchemaVersionModel schema = new SchemaModelImpl();
		schema.setName(RandomStringUtils.random(10, true, true));
		Arrays.stream(fields).forEach(field -> schema.addField(field));
		return schema;
	}
}
