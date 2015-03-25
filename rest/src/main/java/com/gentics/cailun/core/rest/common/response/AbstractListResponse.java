package com.gentics.cailun.core.rest.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AbstractListResponse {

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

}
