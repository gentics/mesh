package com.gentics.mesh.hibernate.data.domain;

import java.util.Optional;
import java.util.UUID;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.database.HibernateTx;

/**
 * Common contract for container field storeable edges.
 * 
 * @author plyhun
 *
 */
public interface HibFieldEdge extends HibDatabaseElement, Field {

	/**
	 * If the edge is attached to a micronode, get it's root container field name.
	 * 
	 * @return
	 */
	default Optional<String> maybeGetFieldNameFromMicronodeParent() {
		if (ReferenceType.MICRONODE == getContainerType()) {
			HibernateTx tx = HibernateTx.get();
			String rootEdgeKey = tx.contentDao().getFieldContainer(tx.load(getContainerVersionUuid(), HibMicroschemaVersionImpl.class), getContainerUuid())
					.getContainerEdges().map(edge -> {
						if (edge instanceof AbstractHibListFieldEdgeImpl) {
							return ((AbstractHibListFieldEdgeImpl<?>) edge).getFieldName();
						} else {
							return edge.getFieldKey();
						}
					}).findAny().orElseThrow(() -> new IllegalStateException("Cannot find the edge for the micronode: " + getContainerUuid()));
			return Optional.of(rootEdgeKey);
		} else {
			return Optional.empty();
		}		
	}

	/**
	 * Runs before this edge is deleted.
	 * 
	 * @param tx the transaction
	 * @param bac action content to report into about the possible data changes.
	 */
	void onEdgeDeleted(HibernateTx tx, BulkActionContext bac);

	/**
	 * Get the owner container type of this edge.
	 * 
	 * @return
	 */
	ReferenceType getContainerType();

	/**
	 * Get the owner container version UUID of this edge.
	 * 
	 * @return
	 */
	UUID getContainerVersionUuid();

	/**
	 * Get the owner container UUID of this edge
	 * 
	 * @return
	 */
	UUID getContainerUuid();
}
