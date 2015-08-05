package com.gentics.mesh.graphdb;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseThreadLocalFactory;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;

public class MeshRecordFactory implements ODatabaseThreadLocalFactory {

	private final OPartitionedDatabasePoolFactory poolFactory;

	public MeshRecordFactory(OPartitionedDatabasePoolFactory poolFactory) {
		this.poolFactory = poolFactory;
	}

	@Override
	public ODatabaseDocumentInternal getThreadDatabase() {
		return poolFactory.get("memory:tinkerpop", "admin", "admin").acquire();
	}

}