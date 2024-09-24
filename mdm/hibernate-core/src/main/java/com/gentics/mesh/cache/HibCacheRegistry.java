package com.gentics.mesh.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.database.HibernateDatabase;

/**
 * Hibernate-based cache registry implementation.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibCacheRegistry extends AbstractCacheRegistry {
	private final HibernateDatabase database;

	@Inject
	public HibCacheRegistry(HibernateDatabase database) {
		super();
		this.database = database;
	}

	@Override
	public void clear() {
		super.clear();
		database.evictAll();
	}
}
