package com.gentics.mesh.test.context;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.db.Tx;

public interface WrapperHelper {

	default Schema createSchema(Tx tx) {
		return tx.getGraph().addFramedVertex(SchemaContainerImpl.class);
	}

	default HibSchemaVersion createSchemaVersion(Tx tx) {
		SchemaContainerVersionImpl graphVersion = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		return graphVersion;
	}

	default Microschema createMicroschema(Tx tx) {
		return tx.getGraph().addFramedVertex(MicroschemaContainerImpl.class);
	}

	default HibMicroschemaVersion createMicroschemaVersion(Tx tx) {
		MicroschemaContainerVersionImpl graphVersion = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
		return graphVersion;
	}
}
