package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibBaseElement;

import dagger.Lazy;

/**
 * Abstract implementation for DAO's.
 * 
 * @param <T>
 */
public abstract class AbstractDaoWrapper<T extends HibBaseElement> implements Dao<T> {

	protected final Lazy<OrientDBBootstrapInitializer> boot;

	public AbstractDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot) {
		this.boot = boot;
	}
}
