package com.gentics.mesh.core.data;

import java.util.Iterator;

import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * A node field container is an aggregation node that holds localized fields (e.g.: StringField, NodeField...)
 */
public interface NodeGraphFieldContainer extends HibNodeFieldContainer, GraphFieldContainer, EditorTrackingVertex, GraphDBBucketableElement {

	/**
	 * Return an iterator over the edges for the given type and branch.
	 * 
	 * @param type
	 * @param branchUuid
	 * @return
	 */
	Iterator<GraphFieldContainerEdge> getContainerEdge(ContainerType type, String branchUuid);

	@Override
	default boolean isValid() {
		return getElement() != null;
	}
}
