package com.gentics.mesh.core.rest.common;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * RestModel POJO for list responses.
 *
 * @param <T>
 */
public class ListResponse<T> implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Array which contains the found elements.")
	private List<T> data = new ArrayList<>();

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

	/**
	 * Return the list data.
	 * 
	 * @return List data
	 */
	// @JsonTypeInfo(use = Id.CLASS)
	// @JsonTypeInfo(use = Id.NONE )
	public List<T> getData() {
		return data;
	}

}
