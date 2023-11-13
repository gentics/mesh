package com.gentics.mesh.core.data.dao;

import java.util.Optional;

import com.gentics.mesh.cache.NameCache;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.db.CommonTx;
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
	default void uncache(T entity) {
		maybeGetCache().ifPresent(cache -> cache.clear(entity.getName()));
	}

	/**
	 * Chear the cached name, along with sending the specified event.
	 * 
	 * @param entity
	 * @param event
	 */
	default void recache(T entity, MeshEventModel event) {
		uncache(entity);
		Tx.maybeGet().map(CommonTx.class::cast).map(ctx -> ctx.data().getOrCreateEventQueueBatch()).ifPresent(bp -> bp.add(event));
	}
}
