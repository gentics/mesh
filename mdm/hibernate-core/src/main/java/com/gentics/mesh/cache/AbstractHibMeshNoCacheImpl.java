package com.gentics.mesh.cache;

import java.util.function.Function;

/**
 * Hibernate has a cache of its own, so we need no app level cache.
 * 
 * @see MeshCache
 */
public abstract class AbstractHibMeshNoCacheImpl<K, V> implements MeshCache<K, V> {

	public AbstractHibMeshNoCacheImpl() {
	}

	@Override
	public void clear() {
		// no cache - no care
	}

	@Override
	public V get(K key, Function<K, V> mappingFunction) {
		return mappingFunction.apply(key);
	}

	@Override
	public V get(K key) {
		return null;
	}

	@Override
	public boolean isDisabled() {
		return true;
	}

	@Override
	public void enable() {
	}

	@Override
	public void disable() {
	}

	@Override
	public long size() {
		return 0L;
	}

}
