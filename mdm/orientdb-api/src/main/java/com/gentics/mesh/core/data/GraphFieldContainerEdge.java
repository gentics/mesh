package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import java.util.Set;

/**
 * Interface for edges between i18n field containers and the node. Edges are language specific, are bound to branches and are either of type "Initial, Draft or
 * Published"
 */
public interface GraphFieldContainerEdge extends MeshEdge, HibNodeFieldContainerEdge {

	// Webroot index

	String WEBROOT_PROPERTY_KEY = "webrootPathInfo";

	String WEBROOT_INDEX_POSTFIX_NAME = "webrootPathInfoIndex";

	String WEBROOT_INDEX_NAME = ("e." + HAS_FIELD_CONTAINER + "_" + WEBROOT_INDEX_POSTFIX_NAME).toLowerCase();

	// Url Field index

	String WEBROOT_URLFIELD_PROPERTY_KEY = "webrootUrlInfo";

	String WEBROOT_URLFIELD_INDEX_POSTFIX_NAME = "webrootUrlInfoIndex";

	String WEBROOT_URLFIELD_INDEX_NAME = ("e." + HAS_FIELD_CONTAINER + "_" + WEBROOT_URLFIELD_INDEX_POSTFIX_NAME).toLowerCase();

	String LANGUAGE_TAG_KEY = "languageTag";

	String BRANCH_UUID_KEY = "branchUuid";

	String EDGE_TYPE_KEY = "edgeType";

	/**
	 * Reset the webroot properties.
	 */
	default void defaultClearDraftPaths() {
		property(WEBROOT_PROPERTY_KEY, null);
		property(WEBROOT_URLFIELD_PROPERTY_KEY, null);
	}

	/**
	 * Return the referenced content as basic field container.
	 * 
	 * @return
	 */
	BasicFieldContainer getContainer();

	/**
	 * Set the branch Uuid
	 * 
	 * @param uuid
	 *            branch Uuid
	 */
	void setBranchUuid(String uuid);

	/**
	 * Set the webroot segment info for the edge.
	 * 
	 * @param segmentInfo
	 */
	@Override
	default void setSegmentInfo(String segmentInfo) {
		property(WEBROOT_PROPERTY_KEY, segmentInfo);
	}

	/**
	 * Return the webroot segment info.
	 * 
	 * @return
	 */
	default String getSegmentInfo() {
		return property(WEBROOT_PROPERTY_KEY);
	}

	/**
	 * Set the url field info.
	 * 
	 * @param urlFieldInfo
	 */
	@Override
	default void setUrlFieldInfo(Set<String> urlFieldInfo) {
		property(WEBROOT_URLFIELD_PROPERTY_KEY, urlFieldInfo);
	}

	/**
	 * Return the url field info.
	 * 
	 * @return
	 */
	default Set<String> getUrlFieldInfo() {
		return property(WEBROOT_URLFIELD_PROPERTY_KEY);
	}

}
