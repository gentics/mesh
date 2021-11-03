package com.gentics.mesh.core.data.schema.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.UpdateMicroschemaChange;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;

/**
 * @see UpdateMicroschemaChange
 */
public class UpdateMicroschemaChangeImpl extends AbstractFieldSchemaContainerUpdateChange<MicroschemaModel> implements UpdateMicroschemaChange {

	/**
	 * Intialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(UpdateMicroschemaChangeImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void delete(BulkActionContext context) {
		getElement().remove();
	}

}
