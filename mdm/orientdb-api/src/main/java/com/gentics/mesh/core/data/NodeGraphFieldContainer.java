package com.gentics.mesh.core.data;

import java.util.Iterator;

import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * A node field container is an aggregation node that holds localized fields (e.g.: StringField, NodeField...)
 */
public interface NodeGraphFieldContainer extends HibNodeFieldContainer, GraphFieldContainer, EditorTrackingVertex, GraphDBBucketableElement {

	/**
	 * Retrieve a conflicting edge for the given segment info, branch uuid and type, or null if there's no conflicting
	 * edge
	 * @param segmentInfo
	 * @param branchUuid
	 * @param type
	 * @param edge
	 * @return the conflicting edge, or null if no conflicting edge exists
	 */
	HibNodeFieldContainerEdge getConflictingEdgeOfWebrootPath(String segmentInfo, String branchUuid, ContainerType type, GraphFieldContainerEdge edge);

	/**
	 * Retrieve a conflicting edge for the given urlFieldValue, branch uuid and type, or null if there's no conflicting
	 * edge
	 * @param edge
	 * @param urlFieldValue
	 * @param branchUuid
	 * @param type
	 * @return the conflicting edge, or null if no conflicting edge exists
	 */
	HibNodeFieldContainerEdge getConflictingEdgeOfWebrootField(GraphFieldContainerEdge edge, String urlFieldValue, String branchUuid, ContainerType type);

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
