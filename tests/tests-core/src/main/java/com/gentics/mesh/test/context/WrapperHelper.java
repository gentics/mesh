package com.gentics.mesh.test.context;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;

/**
 * Helper to deal with tests which need to create entitites and tests which invoke non-mdm code.
 * TODO MDM: This code must be refactored to support MDM
 */
public interface WrapperHelper {

	default HibSchema createSchema(Tx tx) {
		HibSchema schema = ((CommonTx) tx).schemaDao().createPersisted(null);
		schema.generateBucketId();
		return schema;
	}

	default HibSchemaVersion createSchemaVersion(Tx tx) {
		SchemaContainerVersionImpl graphVersion = ((CommonTx) tx).create(SchemaContainerVersionImpl.class);
		return graphVersion;
	}

	default HibMicroschema createMicroschema(Tx tx) {
		HibMicroschema schema = ((CommonTx) tx).microschemaDao().createPersisted(null);
		schema.generateBucketId();
		return schema;
	}

	default HibMicroschemaVersion createMicroschemaVersion(Tx tx) {
		MicroschemaContainerVersionImpl graphVersion = ((CommonTx) tx).create(MicroschemaContainerVersionImpl.class);
		return graphVersion;
	}
}
