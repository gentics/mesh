package com.gentics.mesh.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OrientdbCacheRegistry extends AbstractCacheRegistry {

	@Inject
	public OrientdbCacheRegistry() {
		super();
	}
}
