package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.GraphDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibBaseElement;

import dagger.Lazy;

/**
 * Abstract implementation for DAO's.
 * 
 * @param <T>
 */
public abstract class AbstractDaoWrapper<T extends HibBaseElement> implements Dao<T> {

	protected final Lazy<GraphDBBootstrapInitializer> boot;

	public AbstractDaoWrapper(Lazy<GraphDBBootstrapInitializer> boot) {
		this.boot = boot;
	}
}
