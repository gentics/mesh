package com.gentics.mesh.core.data.schema.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.FieldTypeChange;

/**
 * @see FieldTypeChange
 */
public class FieldTypeChangeImpl extends AbstractSchemaFieldChange implements FieldTypeChange {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(FieldTypeChangeImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

}
