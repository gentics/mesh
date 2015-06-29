package com.gentics.mesh.core.rest.schema.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.response.FieldTypes;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;

public class ListFieldSchemaImpl extends AbstractFieldSchema implements ListFieldSchema {

	private Integer min;
	private Integer max;

	@JsonProperty("allowed")
	private String[] allowedSchemas;

	private String listType;

	@Override
	public String[] getAllowedSchemas() {
		return allowedSchemas;
	}

	@Override
	public void setAllowedSchemas(String[] allowedSchemas) {
		this.allowedSchemas = allowedSchemas;
	}

	@Override
	public String getListType() {
		return listType;
	}

	@Override
	public void setListType(String listType) {
		this.listType = listType;
	}

	@Override
	public Integer getMax() {
		return max;
	}

	@Override
	public void setMax(Integer max) {
		this.max = max;
	}

	@Override
	public Integer getMin() {
		return min;
	}

	@Override
	public void setMin(Integer min) {
		this.min = min;
	}

	@Override
	public String getType() {
		return FieldTypes.LIST.toString();
	}

}
