package com.gentics.mesh.test.context;

import java.util.function.Consumer;

import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;

/**
 * Helper to deal with tests which need to create entitites and tests which invoke non-mdm code.
 */
public interface WrapperHelper {

	default Schema createSchema(Tx tx) {
		Schema schema = ((CommonTx) tx).schemaDao().createPersisted(null, s -> {
			s.setName(s.getUuid());
		});
		schema.generateBucketId();
		return schema;
	}

	default SchemaVersion createSchemaVersion(Tx tx, Schema schema, Consumer<SchemaVersion> inflater) {
		SchemaVersion version = ((CommonTx) tx).schemaDao().createPersistedVersion(schema, inflater);
		return version;
	}

	default Microschema createMicroschema(Tx tx) {
		Microschema schema = ((CommonTx) tx).microschemaDao().createPersisted(null, s -> {
			s.setName(s.getUuid());
		});
		schema.generateBucketId();
		return schema;
	}

	default MicroschemaVersion createMicroschemaVersion(Tx tx, Microschema schema, Consumer<MicroschemaVersion> inflater) {
		MicroschemaVersion version = ((CommonTx) tx).microschemaDao().createPersistedVersion(schema, inflater);
		return version;
	}
}
