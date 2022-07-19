package com.gentics.mesh.util;

import java.util.Arrays;
import java.util.UUID;

import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
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
		return ctx.contentDao().createPersisted(UUID.randomUUID().toString(), version, null, null, new VersionNumber(), null);
	}

	public static SchemaVersionModel createSchema(FieldSchema... fields) {
		SchemaVersionModel schema = new SchemaModelImpl();
		schema.setName(RandomStringUtils.random(10, true, true));
		Arrays.stream(fields).forEach(field -> schema.addField(field));
		return schema;
	}

	/**
	 * Create a schema.
	 *
	 * @param container
	 *            Parent schema container for versions
	 * @param name
	 *            schema name
	 * @param version
	 *            schema version
	 * @param fields
	 *            list of schema fields
	 * @return schema container
	 */
	public static HibSchemaVersion createSchemaVersion(HibSchema container, String name, String version, FieldSchema... fields) {
		HibSchemaVersion sversion = CommonTx.get().schemaDao().createPersistedVersion(container, v -> fillSchemaVersion(v, container, name, version, fields));
		Tx.get().commit();
		return sversion;
	}

	public static HibSchemaVersion fillSchemaVersion(HibSchemaVersion containerVersion, HibSchema container, String name, String versionName, FieldSchema... fields) {
		SchemaVersionModel schema = new SchemaModelImpl();
		schema.setName(name);
		schema.setVersion(versionName);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
		schema.setContainer(false);
		// schema.setDisplayField("name");
		// schema.setSegmentField("name");
		schema.validate();

		containerVersion.setName(name);
		containerVersion.setSchema(schema);
		containerVersion.setSchemaContainer(container);
		return containerVersion;
	}

	public static HibMicroschemaVersion fillMicroschemaVersion(HibMicroschemaVersion microschemaVersion, HibMicroschema microschema, String name, String versionName, FieldSchema... fields) {
		MicroschemaVersionModel model = new MicroschemaModelImpl();
		model.setName(name);
		model.setVersion(versionName);
		for (FieldSchema field : fields) {
			model.addField(field);
		}
		model.validate();

		microschemaVersion.setName(name);
		microschemaVersion.setSchema(model);
		microschemaVersion.setSchemaContainer(microschema);

		return microschemaVersion;
	}
}
