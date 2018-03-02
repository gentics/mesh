package com.gentics.mesh.core.data;

/**
 * Interface for edges between i18n field containers and the node. Edges are language specific, are bound to releases and are either of type "Initial, Draft or
 * Published"
 */
public interface GraphFieldContainerEdge {

	String LANGUAGE_TAG_KEY = "languageTag";

	String RELEASE_UUID_KEY = "releaseUuid";

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
	 * Get the release Uuid
	 * 
	 * @return release Uuid
	 */
	String getReleaseUuid();

	/**
	 * Set the release Uuid
	 * 
	 * @param uuid
	 *            release Uuid
	 */
	void setReleaseUuid(String uuid);

}
