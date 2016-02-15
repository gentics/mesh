package com.gentics.mesh.core.data.schema.impl;

import java.io.IOException;

import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * @see RemoveFieldChange
 */
public class RemoveFieldChangeImpl extends AbstractSchemaFieldChange implements RemoveFieldChange {

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	public FieldSchemaContainer apply(FieldSchemaContainer container) {
		container.removeField(getFieldName());
		return container;
	}

	@Override
	public String getAutoMigrationScript() throws IOException {
		return OPERATION.getAutoMigrationScript(null);
	}

}
