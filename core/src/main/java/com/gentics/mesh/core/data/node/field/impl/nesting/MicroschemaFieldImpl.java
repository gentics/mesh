package com.gentics.mesh.core.data.node.field.impl.nesting;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;

import java.util.List;

import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.impl.NodeFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.nesting.AbstractComplexField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableField;

public class MicroschemaFieldImpl extends AbstractComplexField implements MicroschemaField, FieldContainer {

	@Override
	public <T extends MicroschemaListableField> List<? extends T> getFields() {
		List<? extends NodeFieldContainer> list = out(HAS_FIELD_CONTAINER).has(NodeFieldContainerImpl.class).toListExplicit(
				NodeFieldContainerImpl.class);
		return null;
	}

}
