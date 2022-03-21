package com.gentics.mesh.test.context;

import java.util.function.Consumer;

import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;

/**
 * Helper to deal with tests which need to create entitites and tests which invoke non-mdm code.
 */
public interface WrapperHelper {

	default HibSchema createSchema(Tx tx) {
		HibSchema schema = ((CommonTx) tx).schemaDao().createPersisted(null);
		schema.generateBucketId();
		return schema;
	}

	default HibSchemaVersion createSchemaVersion(Tx tx, HibSchema schema, Consumer<HibSchemaVersion> inflater) {
		HibSchemaVersion version = ((CommonTx) tx).schemaDao().createPersistedVersion(schema, inflater);
		return version;
	}

	default HibMicroschema createMicroschema(Tx tx) {
		HibMicroschema schema = ((CommonTx) tx).microschemaDao().createPersisted(null);
		schema.generateBucketId();
		return schema;
	}

	default HibMicroschemaVersion createMicroschemaVersion(Tx tx, HibMicroschema schema, Consumer<HibMicroschemaVersion> inflater) {
		HibMicroschemaVersion version = ((CommonTx) tx).microschemaDao().createPersistedVersion(schema, inflater);
		return version;
	}
}
