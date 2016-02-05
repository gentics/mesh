package com.gentics.mesh.core.rest.schema.impl;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

public class NodeFieldSchemaImpl extends AbstractFieldSchema implements NodeFieldSchema {

	@JsonProperty("allow")
	private String[] allowedSchemas;

	@Override
	public String[] getAllowedSchemas() {
		return allowedSchemas;
	}

	@Override
	public void setAllowedSchemas(String... allowedSchemas) {
		this.allowedSchemas = allowedSchemas;
	}

	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}

	@Override
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) {

		if (fieldSchema instanceof NodeFieldSchema) {
			NodeFieldSchema nodeFieldSchema = (NodeFieldSchema) fieldSchema;
			boolean modified = false;
			SchemaChangeModel change = new SchemaChangeModel(SchemaChangeOperation.UPDATEFIELD, fieldSchema.getName());

			// required flag:
			modified = compareRequiredField(change, nodeFieldSchema, modified);

			// allow property:
			if (!Arrays.equals(getAllowedSchemas(), nodeFieldSchema.getAllowedSchemas())) {
				change.getProperties().put("allow", nodeFieldSchema.getAllowedSchemas());
				modified = true;
			}

			if (modified) {
				return Optional.of(change);
			}

		} else {
			//TODO impl
		}

		return Optional.empty();
	}

}
