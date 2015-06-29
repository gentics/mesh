package com.gentics.mesh.core.data.model.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_FIELD;

import com.gentics.mesh.core.data.model.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.model.node.field.impl.nesting.MicroschemaFieldImpl;
import com.gentics.mesh.core.data.model.node.field.nesting.MicroschemaField;

public class MeshNodeFieldContainerImpl extends AbstractFieldContainerImpl implements MeshNodeFieldContainer {

	@Override
	public MicroschemaField createMicroschema(String key) {
		MicroschemaFieldImpl field = getGraph().addFramedVertex(MicroschemaFieldImpl.class);
		field.setFieldKey(key);
		linkOut(field, HAS_FIELD);
		return field;
	}
	
	@Override
	public MicroschemaField getMicroschema(String key) {
		// TODO Auto-generated method stub
		return null;
	}
}
