package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.UUID;
import java.util.function.Supplier;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.ContentInterceptor;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Micronode field implementation for Hibernate.
 * 
 * @author plyhun
 *
 */
public class HibMicronodeFieldImpl extends AbstractReferenceHibField<HibMicronodeFieldEdgeImpl> implements HibMicronodeField {

	public HibMicronodeFieldImpl(HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent,	HibMicronodeFieldEdgeImpl initialValue) {
		super(initialValue.getFieldKey(), parent, FieldTypes.MICRONODE, initialValue);
	}

	public HibMicronodeFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent,	UUID initialValue) {
		super(fieldKey, parent, FieldTypes.MICRONODE, initialValue, HibMicronodeFieldEdgeImpl.class);
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?,?,?,?,?>) container;
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.MICRONODE);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getMicronode, false);
		return new HibMicronodeFieldImpl(
				unmanagedBase, 
				HibMicronodeFieldEdgeImpl.fromContainer(tx, unmanagedBase, getFieldKey(), getMicronode()));
	}

	@Override
	public HibMicronodeContainerImpl getMicronode() {
		Supplier<HibMicronode> defaultSupplier = () -> {
			HibMicronodeFieldEdgeImpl referenced = getReferencedEdge();
			return referenced != null ? referenced.getMicronode() : null;
		};

		// use the dataloader, which was prepared in the content interceptor to load the micronode (this will enable batch loading)
		// use the default supplier, if the dataloader did not load the micronode
		HibernateTx tx = HibernateTx.get();
		ContentInterceptor contentInterceptor = tx.getContentInterceptor();
		HibMicronode micronode = contentInterceptor.getDataLoaders().map(loaders -> loaders.getMicronode(this, defaultSupplier)).orElseGet(defaultSupplier);

		if (micronode instanceof HibMicronodeContainerImpl) {
			return HibMicronodeContainerImpl.class.cast(micronode);
		} else {
			return null;
		}
	}

	@Override
	public boolean equals(Object object) {
		return micronodeFieldEquals(object);
	}

	@Override
	public void validate() {
		getReferencedEdge().validate();
	}

	@Override
	public HibMicronodeFieldEdgeImpl getReferencedEdge() {
		return HibernateTx.get().entityManager()
				.find(HibMicronodeFieldEdgeImpl.class, value.get());

	}
}
