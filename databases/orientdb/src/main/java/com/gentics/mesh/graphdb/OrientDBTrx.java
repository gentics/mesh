package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientDBTrx extends AbstractTrx {

	public OrientDBTrx(Database database) {
		init(database, database.startTransaction());
	}

}
