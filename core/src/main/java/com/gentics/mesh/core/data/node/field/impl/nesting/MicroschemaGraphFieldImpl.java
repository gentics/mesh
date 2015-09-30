package com.gentics.mesh.core.data.node.field.impl.nesting;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import java.util.List;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.nesting.AbstractComplexGraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaGraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;

//TODO finish implementation
public class MicroschemaGraphFieldImpl extends AbstractComplexGraphField implements MicroschemaGraphField, GraphFieldContainer {

	@Override
	public <T extends MicroschemaListableGraphField> List<? extends T> getFields() {
		List<? extends NodeGraphFieldContainer> list = out(HAS_FIELD_CONTAINER).has(NodeGraphFieldContainerImpl.class)
				.toListExplicit(NodeGraphFieldContainerImpl.class);
		return null;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
	}

}
