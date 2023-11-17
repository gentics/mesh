package com.gentics.mesh.core.data.dao;

import java.util.Optional;

import com.gentics.mesh.cache.NameCache;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.MeshEventModel;

/**
 * An extra DAO layer for named entities.
 * 
 * @param <T>
 */
public interface PersistingNamedEntityDao<T extends HibNamedElement> {

	/**
	 * Get the named entity cache, if supported.
	 * 
	 * @return
	 */
	default Optional<NameCache<T>> maybeGetCache() {
		return Optional.empty();
	}

	/**
	 * Clear the cached name.
	 * 
	 * @param entity
	 */
	default void uncacheSync(T entity) {
		Tx.maybeGet().flatMap(tx -> maybeGetCache()).ifPresent(cache -> cache.clear(entity.getName()));
	}

	/**
	 * Clear the cached name, along with sending the specified event.
	 * 
	 * @param event
	 */
	default void addBatchEvent(MeshEventModel event) {
		Tx.maybeGet().map(tx -> tx.createBatch()).ifPresent(bp -> bp.add(event));
	}
}
