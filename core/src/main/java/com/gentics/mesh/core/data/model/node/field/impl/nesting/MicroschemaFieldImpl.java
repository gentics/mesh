package com.gentics.mesh.core.data.model.node.field.impl.nesting;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_FIELD_CONTAINER;

import java.util.List;

import com.gentics.mesh.core.data.model.FieldContainer;
import com.gentics.mesh.core.data.model.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.nesting.AbstractComplexField;
import com.gentics.mesh.core.data.model.node.field.nesting.MicroschemaField;
import com.gentics.mesh.core.data.model.node.field.nesting.MicroschemaListableField;

public class MicroschemaFieldImpl extends AbstractComplexField implements MicroschemaField, FieldContainer {

	@Override
	public <T extends MicroschemaListableField> List<? extends T> getFields() {
		List<? extends MeshNodeFieldContainer> list = out(HAS_FIELD_CONTAINER).has(MeshNodeFieldContainerImpl.class).toListExplicit(
				MeshNodeFieldContainerImpl.class);
		return null;
	}

}
