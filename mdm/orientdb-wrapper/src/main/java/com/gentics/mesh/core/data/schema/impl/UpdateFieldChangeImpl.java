package com.gentics.mesh.core.data.schema.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * @see UpdateFieldChange
 */
public class UpdateFieldChangeImpl extends AbstractSchemaFieldChange implements UpdateFieldChange {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(UpdateFieldChangeImpl.class, MeshVertexImpl.class);
	}
	@Override
	public void setRestProperty(String key, Object value) {
		// What a restriction removal request comes from the REST API,
		// it gives null instead of an empty array, so the request
		// gets eventually lost. In this case we give the empty array back.
		if (SchemaChangeModel.ALLOW_KEY.equals(key) && value == null) {
			value = new String[0];
		}
		super.setRestProperty(key, value);
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}
}
