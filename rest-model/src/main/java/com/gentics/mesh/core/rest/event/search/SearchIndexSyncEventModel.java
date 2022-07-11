package com.gentics.mesh.core.rest.event.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;

/**
 * Model of the event, which is sent to initiate an index sync
 */
public class SearchIndexSyncEventModel extends AbstractMeshEventModel {
	@JsonProperty(required = true)
	@JsonPropertyDescription("Index pattern")
	private String indexPattern = ".*";

	/**
	 * Create empty instance
	 */
	public SearchIndexSyncEventModel() {
	}

	/**
	 * Get the index pattern
	 * @return index pattern
	 */
	public String getIndexPattern() {
		return indexPattern;
	}

	/**
	 * Set the index pattern
	 * @param indexPattern index pattern
	 * @return fluent API
	 */
	public SearchIndexSyncEventModel setIndexPattern(String indexPattern) {
		if (indexPattern != null) {
			this.indexPattern = indexPattern;
		}
		return this;
	}
}
