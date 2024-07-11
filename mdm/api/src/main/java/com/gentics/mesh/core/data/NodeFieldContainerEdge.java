package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * An edge entity, which connects a {@link Node} with the {@link NodeFieldContainer} 
 * based on the language tag, branch and {@link ContainerType}.
 * 
 * @author plyhun
 *
 */
public interface NodeFieldContainerEdge {

	/**
	 * Set the webroot segment info for the edge.
	 *
	 * @param segmentInfo
	 */
	void setSegmentInfo(String segmentInfo);

	/**
	 * Get the webroot segment info
	 * @return webroot segment info
	 */
	String getSegmentInfo();

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
	NodeFieldContainer getNodeContainer();

	/**
	 * Return the node from which this edge originates.
	 *
	 * @return
	 */
	Node getNode();

	/**
	 * Get the language tag
	 * 
	 * @return language tag
	 */
	String getLanguageTag();

	/**
	 * Set the language tag.
	 * 
	 * @param languageTag
	 */
	void setLanguageTag(String languageTag);

	/**
	 * Get the edge type
	 * 
	 * @return edge type
	 */
	ContainerType getType();

	/**
	 * Set the edge type
	 * 
	 * @param type
	 *            edge type
	 */
	void setType(ContainerType type);

	/**
	 * Get the branch Uuid
	 * 
	 * @return branch Uuid
	 */
	String getBranchUuid();

	/**
	 * Set the segment info which consists of :nodeUuid + "-" + segment. The property is indexed and used for the webroot path resolving mechanism.
	 * 
	 * @param parentNode
	 * @param segment
	 */
	default void setSegmentInfo(Node parentNode, String segment) {
		setSegmentInfo(Tx.get().contentDao().composeSegmentInfo(parentNode, segment));
	}
}
