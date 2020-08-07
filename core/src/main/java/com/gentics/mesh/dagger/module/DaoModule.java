package com.gentics.mesh.dagger.module;

import javax.inject.Singleton;

import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.dao.impl.OrientDBDaoCollection;

import dagger.Module;
import dagger.Provides;

@Module
public class DaoModule {

	@Provides
	@Singleton
	public static DaoCollection daoCollection(OrientDBDaoCollection daoCollection) {
		// Switch here between different dao implementations
		return daoCollection;
	}
	
}
