package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeFieldCommon;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.syncleus.ferma.VertexFrame;

/**
 * @see NodeGraphField
 */
public class NodeGraphFieldImpl extends MeshEdgeImpl implements NodeGraphField {

	@Override
	public String getFieldKey() {
		return property(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public void setFieldKey(String key) {
		property(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public Stream<? extends HibNodeFieldContainer> getReferencingContents() {
		return getReferencingFieldContainers()
			.flatMap(GraphFieldContainer::getContents);
	}

	/**
	 * Creates a stream of all field containers that are connected to this edge.
	 * 
	 * @return
	 */
	private Stream<GraphFieldContainer> getReferencingFieldContainers() {
		if (label().equals(HAS_FIELD)) {
			return toStream(outV().frame(GraphFieldContainer.class));
		} else { // if HAS_ITEM
			return toStream(outV().in(HAS_LIST).frame(GraphFieldContainer.class));
		}
	}

	@Override
	public Node getNode() {
		return inV().nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		// TODO BUG We must only remove one edge to the given container!
		remove();
	}

	@Override
	public GraphField cloneTo(HibFieldContainer container) {
		NodeGraphFieldImpl field = getGraph().addFramedEdge(toGraph(container), getNode(), HAS_FIELD, NodeGraphFieldImpl.class);
		field.setFieldKey(getFieldKey());
		return field;
	}

	@Override
	public void validate() {
	}

	@Override
	public String getFieldName() {
		ListSkipper skipper = new ListSkipper();

		if (skipper.nextVertex instanceof NodeGraphFieldContainer) {
			return skipper.getName();
		} else {
			return skipper.nextVertex.inE(HAS_FIELD, HAS_ITEM)
				.nextExplicit(NodeGraphFieldImpl.class)
				.getFieldName();
		}
	}

	@Override
	public Optional<String> getMicronodeFieldName() {
		ListSkipper skipper = new ListSkipper();

		if (skipper.nextVertex instanceof MicronodeImpl) {
			return Optional.of(skipper.getName());
		} else {
			return Optional.empty();
		}
	}

	@Override
	public boolean equals(Object obj) {
		return HibNodeFieldCommon.equalsNodeField(this, obj);
	}

	/**
	 * Used in {@link #getFieldName()} and {@link #getMicronodeFieldName()} to skip lists and abstract the retrieval of the field name.
	 */
	private class ListSkipper {
		VertexFrame nextVertex;
		Supplier<String> nameSupplier;

		private ListSkipper() {
			VertexFrame framedVertex = outV().next();

			if (framedVertex instanceof ListGraphField) {
				nameSupplier = ((ListGraphField) framedVertex)::getFieldKey;
				nextVertex = framedVertex.in(HAS_LIST).next();
			} else {
				nextVertex = framedVertex;
				nameSupplier = NodeGraphFieldImpl.this::getFieldKey;
			}
		}

		public String getName() {
			return nameSupplier.get();
		}
	}

	@Override
	public ReferenceType getReferenceType() {
		VertexFrame framedVertex = outV().next();

		if (framedVertex instanceof ListGraphField) {
			return ReferenceType.LIST;
		} else if (framedVertex instanceof MicronodeImpl) {
			return ReferenceType.MICRONODE;
		} else {
			return ReferenceType.FIELD;
		}
	}
}
