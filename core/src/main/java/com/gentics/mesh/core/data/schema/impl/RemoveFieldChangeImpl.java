package com.gentics.mesh.core.data.schema.impl;

import java.util.Collections;
import java.util.Map;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * @see RemoveFieldChange
 */
public class RemoveFieldChangeImpl extends AbstractSchemaFieldChange implements RemoveFieldChange {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(RemoveFieldChangeImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	public Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		return Collections.emptyMap();
	}

	@Override
	public FieldSchemaContainer apply(FieldSchemaContainer container) {
		container.removeField(getFieldName());
		return container;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

}
