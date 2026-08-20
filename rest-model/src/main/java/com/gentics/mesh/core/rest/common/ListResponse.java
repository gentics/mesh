package com.gentics.mesh.core.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * RestModel POJO for list responses.
 *
 * @param <T>
 */
public class ListResponse<T> extends ListRequest<T> {

	@JsonPropertyDescription("Paging information of the list result.")
	@JsonProperty(value = "_metainfo", required = true)
	private PagingMetaInfo metainfo = new PagingMetaInfo();

	public ListResponse() {
	}

	/**
	 * Return the meta info for the list.
	 * 
	 * @return Meta info
	 */
	public PagingMetaInfo getMetainfo() {
		return metainfo;
	}

	/**
	 * Set the meta info for the list.
	 * 
	 * @param metainfo
	 *            Meta info
	 */
	public void setMetainfo(PagingMetaInfo metainfo) {
		this.metainfo = metainfo;
	}
}
