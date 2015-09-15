package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;

public class OrientDBTrx extends AbstractTrx {

	public OrientDBTrx(Database database) {	
		init(database, database.startTransaction());
	}

}
