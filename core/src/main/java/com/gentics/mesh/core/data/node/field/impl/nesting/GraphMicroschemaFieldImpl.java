package com.gentics.mesh.core.data.node.field.impl.nesting;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import java.util.List;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.nesting.AbstractComplexGraphField;
import com.gentics.mesh.core.data.node.field.nesting.GraphMicroschemaField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GraphMicroschemaFieldImpl extends AbstractComplexGraphField implements GraphMicroschemaField, GraphFieldContainer {

	private static final Logger log = LoggerFactory.getLogger(GraphMicroschemaFieldImpl.class);
	
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
