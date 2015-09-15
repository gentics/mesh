package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;

public class Neo4jTrx extends AbstractTrx {

	public Neo4jTrx(Database database) {
		init(database, database.startTransaction());
	}
}
