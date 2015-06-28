package com.gentics.mesh.core.data.model.node.field.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_FIELD_CONTAINER;

import java.util.List;

import com.gentics.mesh.core.data.model.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.AbstractComplexField;
import com.gentics.mesh.core.data.model.node.field.MicroschemaField;
import com.gentics.mesh.core.data.model.node.field.MicroschemaListable;

public class MicroschemaFieldImpl extends AbstractComplexField implements MicroschemaField {

	@Override
	public List<? extends MicroschemaListable> getFields() {
		List<? extends MeshNodeFieldContainer> list = out(HAS_FIELD_CONTAINER).has(MeshNodeFieldContainerImpl.class).toListExplicit(
				MeshNodeFieldContainerImpl.class);
		return null;
	}

}
