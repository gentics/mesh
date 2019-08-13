package com.gentics.mesh.cache;

import java.util.function.Function;

public abstract class AbstractMeshCache<K, V> implements MeshCache<K, V> {

	protected final EventAwareCache<K, V> cache;
	private boolean enabled = false;

	public AbstractMeshCache(EventAwareCache<K, V> cache, CacheRegistry registry, long maxSize) {
		this.cache = cache;
		this.enabled = maxSize > 0;
		registry.register(cache);
	}

	@Override
	public void clear() {
		cache.invalidate();
	}

	@Override
	public V get(K key, Function<K, V> mappingFunction) {
		if (isDisabled()) {
			return mappingFunction.apply(key);
		}
		return cache.get(key, mappingFunction);
	}

	@Override
	public V get(K key) {
		if (isDisabled()) {
			return null;
		}
		return cache.get(key);
	}

	@Override
	public boolean isDisabled() {
		return !enabled;
	}

	@Override
	public void enable() {
		enabled = true;
	}

	@Override
	public void disable() {
		enabled = false;
	}

	@Override
	public long size() {
		return cache.size();
	}

}
