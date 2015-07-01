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

	public PagingMetaInfo getMetainfo() {
		return metainfo;
	}

	public void setMetainfo(PagingMetaInfo metainfo) {
		this.metainfo = metainfo;
	}

	@JsonTypeInfo(use = Id.CLASS)
	public List<T> getData() {
		return data;
	}

}
