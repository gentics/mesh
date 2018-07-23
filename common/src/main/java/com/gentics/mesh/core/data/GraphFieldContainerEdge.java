package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.Node;
import com.syncleus.ferma.EdgeFrame;

/**
 * Interface for edges between i18n field containers and the node. Edges are language specific, are bound to branches and are either of type "Initial, Draft or
 * Published"
 */
public interface GraphFieldContainerEdge extends EdgeFrame {

	// Webroot index

	String WEBROOT_PROPERTY_KEY = "webrootPathInfo";

	String WEBROOT_INDEX_NAME = "webrootPathInfoIndex";

	String PUBLISHED_WEBROOT_PROPERTY_KEY = "publishedWebrootPathInfo";

	String PUBLISHED_WEBROOT_INDEX_NAME = "publishedWebrootPathInfoIndex";

	// Url Field index

	String WEBROOT_URLFIELD_PROPERTY_KEY = "webrootUrlInfo";

	String WEBROOT_URLFIELD_INDEX_NAME = "webrootUrlInfoIndex";

	String PUBLISHED_WEBROOT_URLFIELD_PROPERTY_KEY = "publishedWebrootUrlInfo";

	String PUBLISHED_WEBROOT_URLFIELD_INDEX_NAME = "publishedWebrootInfoIndex";

	String LANGUAGE_TAG_KEY = "languageTag";

	String BRANCH_UUID_KEY = "branchUuid";

	String EDGE_TYPE_KEY = "edgeType";

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
	 * Set the branch Uuid
	 * 
	 * @param uuid
	 *            branch Uuid
	 */
	void setBranchUuid(String uuid);

	default void defaultClearDraftPaths() {
		setProperty(WEBROOT_PROPERTY_KEY, null);
		setProperty(WEBROOT_URLFIELD_PROPERTY_KEY, null);
	}

	BasicFieldContainer getContainer();

	NodeGraphFieldContainer getNodeContainer();

	/**
	 * Return the node from which this edge originates.
	 * 
	 * @return
	 */
	Node getNode();
}
