package com.gentics.mesh.core.rest.schema.impl;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModelImpl;

public class ListFieldSchemaImpl extends AbstractFieldSchema implements ListFieldSchema {

	private Integer min;
	private Integer max;

	@JsonProperty("allow")
	private String[] allowedSchemas;

	private String listType;

	@Override
	public String[] getAllowedSchemas() {
		return allowedSchemas;
	}

	@Override
	public ListFieldSchema setAllowedSchemas(String[] allowedSchemas) {
		this.allowedSchemas = allowedSchemas;
		return this;
	}

	@Override
	public String getListType() {
		return listType;
	}

	@Override
	public ListFieldSchema setListType(String listType) {
		this.listType = listType;
		return this;
	}

	@Override
	public Integer getMax() {
		return max;
	}

	@Override
	public ListFieldSchema setMax(Integer max) {
		this.max = max;
		return this;
	}

	@Override
	public Integer getMin() {
		return min;
	}

	@Override
	public ListFieldSchema setMin(Integer min) {
		this.min = min;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.LIST.toString();
	}

	@Override
	public Optional<SchemaChangeModelImpl> compareTo(FieldSchema fieldSchema) {
		// TODO Auto-generated method stub
		return null;
	}

}
