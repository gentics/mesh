package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.core.data.node.HibNode;

public interface HibNodeFieldContainerEdge {

	/**
	 * Set the webroot segment info for the edge.
	 *
	 * @param segmentInfo
	 */
	void setSegmentInfo(String segmentInfo);

	/**
	 * Set the url field info.
	 *
	 * @param urlFieldInfo
	 */
	void setUrlFieldInfo(Set<String> urlFieldInfo);

	/**
	 * Return the referenced content.
	 *
	 * @return
	 */
	HibNodeFieldContainer getNodeContainer();

	/**
	 * Return the node from which this edge originates.
	 *
	 * @return
	 */
	HibNode getNode();

}
