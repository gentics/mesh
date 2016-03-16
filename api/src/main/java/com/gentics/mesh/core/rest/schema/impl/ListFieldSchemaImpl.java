package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.google.common.base.Objects;

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
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) throws IOException {
		if (fieldSchema instanceof ListFieldSchema) {
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;

			SchemaChangeModel change = SchemaChangeModel.createUpdateFieldChange(fieldSchema.getName());
			boolean modified = false;

			// required flag:
			modified = compareRequiredField(change, listFieldSchema, modified);

			// type property:
			if (!Objects.equal(getListType(), listFieldSchema.getListType())) {
				change.getProperties().put("listType", listFieldSchema.getListType());
				modified = true;
			}

			// allow property:
			if (!Arrays.equals(getAllowedSchemas(), listFieldSchema.getAllowedSchemas())) {
				change.getProperties().put("allow", listFieldSchema.getAllowedSchemas());
				modified = true;
			}

			// min

			// max

			if (modified) {
				change.loadMigrationScript();
				return Optional.of(change);
			}
		} else {
			return createTypeChange(fieldSchema);
		}
		return Optional.empty();
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		Object allowedSchemas = fieldProperties.get(SchemaChangeModel.ALLOW_KEY);
		if( allowedSchemas!=null) {
			setAllowedSchemas((String[])allowedSchemas);
		}
	}

	@Override
	public void validate() {
		super.validate();
		if (StringUtils.isEmpty(getListType())) {
			throw error(BAD_REQUEST, "schema_error_list_type_missing", getName());
		}
		//TODO the list type should be a enum.
		List<String> validTypes = Arrays.asList("html", "boolean", "string", "micronode", "node", "number", "date", "binary");
		if (!validTypes.contains(getListType())) {
			throw error(BAD_REQUEST, "schema_error_list_type_invalid", getListType(), getName());
		}
	}

}
