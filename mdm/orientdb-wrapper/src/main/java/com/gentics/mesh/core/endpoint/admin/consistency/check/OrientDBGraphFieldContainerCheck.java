package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.rest.common.ContainerType;

public class OrientDBGraphFieldContainerCheck extends GraphFieldContainerCheck<NodeGraphFieldContainer> {

	@Override
	protected Class<? extends NodeGraphFieldContainer> getContainerClass() {
		return NodeGraphFieldContainerImpl.class;
	}

	@Override
	protected boolean isContainerInitialForNode(NodeGraphFieldContainer container) {
		return container.inE(HAS_FIELD_CONTAINER)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode()).frameExplicit(GraphFieldContainerEdgeImpl.class).iterator().hasNext();
	}
}
