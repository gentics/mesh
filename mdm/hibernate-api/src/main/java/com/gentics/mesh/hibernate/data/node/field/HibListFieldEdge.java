package com.gentics.mesh.hibernate.data.node.field;

import java.util.UUID;

import com.gentics.mesh.core.rest.common.ReferenceType;

/**
 * List field item edge entity.
 * 
 * @param <V> actually stored list item type.
 */
public interface HibListFieldEdge<V> {

	/**
	 * Get container join type
	 * 
	 * @return
	 */
	ReferenceType getContainerType();

	/**
	 * Get UUID of an owning container
	 * 
	 * @return
	 */
	UUID getContainerUuid();

	/**
	 * Get UUID of an owning container's schema version.
	 * 
	 * @return
	 */
	UUID getContainerVersionUuid();

	/**
	 * Get the name of the list field this edge belongs to
	 * 
	 * @return
	 */
	String getFieldName();

	/**
	 * Get the value of this item.
	 * 
	 * @return
	 */
	V getValueOrUuid();

	/**
	 * Get internal UUID of the list this item belongs to. 
	 * 
	 * @return
	 */
	UUID getListUuid();

	/**
	 * Get DB record UUID of this item.
	 * 
	 * @return
	 */
	UUID getDbUuid();
}
