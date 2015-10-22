package com.gentics.mesh.core.rest.common;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

public class AbstractListResponse<T> {

	private List<T> data = new ArrayList<>();

	@JsonProperty("_metainfo")
	private PagingMetaInfo metainfo = new PagingMetaInfo();

	public AbstractListResponse() {
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
	@JsonTypeInfo(use = Id.CLASS)
	public List<T> getData() {
		return data;
	}

}
