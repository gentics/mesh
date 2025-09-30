package com.gentics.mesh.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.netty.handler.codec.http.HttpResponseStatus;

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
		schema.setName(RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(10, 20)));
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

	/**
	 * Check whether the throwable is a {@link MeshRestClientMessageException} with {@link MeshRestClientMessageException#getStatusCode()} {@link HttpResponseStatus#CONFLICT}.
	 * @param t throwable
	 * @return true for conflict errors, false otherwise
	 */
	public static boolean isConflict(Throwable t) {
		return isResponseStatus(t, HttpResponseStatus.CONFLICT);
	}

	/**
	 * Check whether the throwable is a {@link MeshRestClientMessageException} with the given {@link MeshRestClientMessageException#getStatusCode()}.
	 * @param t throwable
	 * @param status status code in question
	 * @return true, iff the status code matches
	 */
	public static boolean isResponseStatus(Throwable t, HttpResponseStatus status) {
		return getMeshRestClientMessageException(t).map(meshException -> meshException.getStatusCode() == status.code()).orElse(false);
	}

	/**
	 * Get the optional {@link MeshRestClientMessageException} instance wrapped in the given {@link Throwable}.
	 * @param t throwable
	 * @return optional MeshRestClientMessageException
	 */
	public static Optional<MeshRestClientMessageException> getMeshRestClientMessageException(Throwable t) {
		if (t instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
			return Optional.of(meshException);
		} else if (t.getCause() != null && t.getCause() != t) {
			return getMeshRestClientMessageException(t.getCause());
		} else {
			return Optional.empty();
		}
	}
}
