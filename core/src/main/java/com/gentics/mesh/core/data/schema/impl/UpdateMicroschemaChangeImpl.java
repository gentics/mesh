package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.UpdateMicroschemaChange;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see UpdateMicroschemaChange
 */
public class UpdateMicroschemaChangeImpl extends AbstractFieldSchemaContainerUpdateChange<Microschema> implements UpdateMicroschemaChange {

	public static void init(Database database) {
		database.addVertexType(UpdateMicroschemaChangeImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	public void delete(BulkActionContext context) {
		getElement().remove();
	}

}
