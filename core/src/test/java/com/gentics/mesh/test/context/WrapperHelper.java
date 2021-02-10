package com.gentics.mesh.test.context;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.db.GraphDBTx;

/**
 * Helper to deal with tests which need to create entitites and tests which invoke non-mdm code.
 * TODO MDM: This code must be refactored to support MDM
 */
public interface WrapperHelper {

	default Schema createSchema(Tx tx) {
		Schema schema = ((GraphDBTx) tx).getGraph().addFramedVertex(SchemaContainerImpl.class);
		schema.generateBucketId();
		return schema;
	}

	default HibSchemaVersion createSchemaVersion(Tx tx) {
		SchemaContainerVersionImpl graphVersion = ((GraphDBTx) tx).getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		return graphVersion;
	}

	default HibMicroschema createMicroschema(Tx tx) {
		HibMicroschema schema = ((GraphDBTx) tx).getGraph().addFramedVertex(MicroschemaContainerImpl.class);
		schema.generateBucketId();
		return schema;
	}

	default HibMicroschemaVersion createMicroschemaVersion(Tx tx) {
		MicroschemaContainerVersionImpl graphVersion = ((GraphDBTx) tx).getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
		return graphVersion;
	}
}
