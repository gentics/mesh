package com.gentics.mesh.core.rest.common.response;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;

public class AbstractListResponse<T extends AbstractRestModel> {

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
