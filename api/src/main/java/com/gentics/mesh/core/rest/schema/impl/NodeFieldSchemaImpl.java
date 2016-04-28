package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

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
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException {
		SchemaChangeModel change = super.compareTo(fieldSchema);
		if (fieldSchema instanceof NodeFieldSchema) {
			NodeFieldSchema nodeFieldSchema = (NodeFieldSchema) fieldSchema;

			// allow property:
			if (!Arrays.equals(getAllowedSchemas(), nodeFieldSchema.getAllowedSchemas())) {
				change.setOperation(UPDATEFIELD);
				change.getProperties().put("allow", nodeFieldSchema.getAllowedSchemas());
			}

		} else {
			return createTypeChange(fieldSchema);
		}
		return change;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get("allowedSchemas") != null) {
			setAllowedSchemas((String[]) fieldProperties.get("allowedSchemas"));
		}
	}

}
