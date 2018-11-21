package com.gentics.diktyo.orientdb3;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.diktyo.Diktyo;
import com.gentics.diktyo.db.DatabaseManager;
import com.gentics.diktyo.orientdb3.db.DatabaseManagerImpl;

@Singleton
public class DiktyoImpl implements Diktyo {

	private final DatabaseManager dbManager;

	@Inject
	public DiktyoImpl(DatabaseManagerImpl dbManager) {
		this.dbManager = dbManager;
	}

	@Override
	public DatabaseManager db() {
		return dbManager;
	}

}
