package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeFieldCommon;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibFieldContainerBase;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Node reference field implementation for Hibernate.
 * 
 * @author plyhun
 *
 */
public class HibNodeFieldImpl extends AbstractReferenceHibField<HibNodeFieldEdgeImpl> implements HibNodeFieldCommon {

	public HibNodeFieldImpl(HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, HibNodeFieldEdgeImpl edge) {
		super(edge.getFieldKey(), parent, FieldTypes.NODE, edge);
	}

	public HibNodeFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID edgeUuid) {
		super(fieldKey, parent, FieldTypes.NODE, edgeUuid, HibNodeFieldEdgeImpl.class);
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		return getReferencedEdge() != null ? getReferencedEdge().cloneTo(container) : null;
	}

	@Override
	public HibNodeImpl getNode() {
		return getReferencedEdge() != null ? getReferencedEdge().getNode() : null;
	}

	@Override
	public Stream<? extends HibNodeFieldContainer> getReferencingContents(boolean lookupInContent, boolean lookupInMicronode) {
		return Stream.of(getContainer())
				.filter(content -> (lookupInContent && content instanceof HibNodeFieldContainer) || (lookupInMicronode && content instanceof HibMicronode))
				.flatMap(HibFieldContainerBase::getNodeFieldContainers);
	}

	@Override
	public String getFieldName() {
		return getFieldKey();
	}

	@Override
	public Optional<String> getMicronodeFieldName() {
		return Optional.empty();
	}

	@Override
	public boolean equals(Object obj) {
		return nodeFieldEquals(obj);
	}

	@Override
	public void validate() {
		if (getReferencedEdge() != null) {
			getReferencedEdge().validate();
		}
	}

	@Override
	public HibNodeFieldEdgeImpl getReferencedEdge() {
		return HibernateTx.get().entityManager()
				.find(HibNodeFieldEdgeImpl.class, value.get());
	}
}
