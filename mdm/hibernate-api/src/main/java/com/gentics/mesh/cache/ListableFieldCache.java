package com.gentics.mesh.cache;

import java.util.List;
import java.util.UUID;

import com.gentics.mesh.core.data.node.field.nesting.HibListableField;

/**
 * Interface for the cache containing values of list fields
 *
 * @param <U> type of the {@link HibListableField} implementation
 */
public interface ListableFieldCache<U extends HibListableField> extends MeshCache<UUID, List<? extends U>> {
	/**
	 * Put the given list into the cache
	 * @param listUuid list UUID (cache key)
	 * @param value list of items
	 */
	void put(UUID listUuid, List<? extends U> value);

	/**
	 * Invalidate the cache entry with given list UUID
	 * @param listUuid list UUID
	 */
	void invalidate(UUID listUuid);
}
