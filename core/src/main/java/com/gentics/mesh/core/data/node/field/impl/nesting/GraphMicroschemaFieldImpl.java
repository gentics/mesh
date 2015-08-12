package com.gentics.mesh.core.data.node.field.impl.nesting;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import java.util.List;

import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.impl.NodeFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.nesting.AbstractComplexField;
import com.gentics.mesh.core.data.node.field.nesting.GraphMicroschemaField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GraphMicroschemaFieldImpl extends AbstractComplexField implements GraphMicroschemaField, FieldContainer {

	private static final Logger log = LoggerFactory.getLogger(GraphMicroschemaFieldImpl.class);
	
	@Override
	public <T extends MicroschemaListableGraphField> List<? extends T> getFields() {
		List<? extends NodeFieldContainer> list = out(HAS_FIELD_CONTAINER).has(NodeFieldContainerImpl.class)
				.toListExplicit(NodeFieldContainerImpl.class);
		return null;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

}
