package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import jakarta.persistence.EntityManager;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeField;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldListModel;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Micronode list field implementation.
 * 
 * @author plyhun
 *
 */
public class HibMicronodeListFieldImpl 
			extends AbstractHibListFieldImpl<HibMicronodeListFieldEdgeImpl, MicronodeField, MicronodeFieldListModel, Micronode, UUID> 
			implements MicronodeFieldList {

	/**
	 * The constructor for initializing the list field.
	 * 
	 * @param tx
	 * @param fieldKey
	 * @param parent
	 */
	public HibMicronodeListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent) {
		super(tx, fieldKey, parent, HibMicronodeListFieldEdgeImpl.class);
	}

	/**
	 * The content value fetched constructor.
	 * 
	 * @param fieldKey
	 * @param parent
	 * @param listUuid
	 */
	public HibMicronodeListFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent,	UUID listUuid) {
		super(listUuid, fieldKey, parent, HibMicronodeListFieldEdgeImpl.class);
	}

	/**
	 * Make a list for the given container, field key and values.
	 * 
	 * @param tx
	 * @param container
	 * @param fieldKey
	 * @param values
	 * @return
	 */
	public static HibMicronodeListFieldImpl fromContainer(HibernateTx tx, HibUnmanagedFieldContainer<?,?,?,?,?> container,
			String fieldKey, List<Micronode> values) {
		HibMicronodeListFieldImpl list = new HibMicronodeListFieldImpl(tx, fieldKey, container);
		IntStream.range(0, values.size())
			.mapToObj(i -> new HibMicronodeListFieldEdgeImpl(tx, list.valueOrNull(), i, fieldKey, (UUID) values.get(i).getId(), values.get(i).getSchemaContainerVersion(), container))
			.forEach(item -> tx.entityManager().persist(item));
		return list;
	}

	@Override
	// TODO do we need real BulkActionContext here?
	public void insertReferenced(int index, Micronode value) {
		makeFromValueAndIndex(value, index, HibernateTx.get());
	}

	@Override
	// TODO do we need real BulkActionContext here?
	public void deleteReferenced(Micronode value) {
		HibernateTx tx = HibernateTx.get();		
		if (value != null && tx.entityManager().createNamedQuery("micronodelistitem.deleteByNodeUuidVersion")
				.setParameter("micronodeUuid", value.getId())
				.setParameter("micronodeVersion", value.getSchemaContainerVersion())
				.executeUpdate() == 1) {
			HibernateTx.get().contentDao().delete(value, new DummyBulkActionContext());
		} else {
			MicronodeField.log.debug("The micronode { " + value + " } has not been deleted");
		}
	}

	@Override
	// TODO do we need real BulkActionContext here?
	public Micronode createMicronodeAt(Optional<Integer> maybeIndex, MicroschemaVersion microschemaVersion) {
		HibernateTx tx = HibernateTx.get();

		HibMicronodeContainerImpl micronode = new HibMicronodeContainerImpl();
		micronode.setDbUuid(tx.uuidGenerator().generateType1UUID());
		micronode.setSchemaContainerVersion(microschemaVersion);
		micronode.put(CommonContentColumn.DB_VERSION, 1L);
		micronode.put(CommonContentColumn.SCHEMA_VERSION_DB_UUID, microschemaVersion.getId());
		micronode.put(CommonContentColumn.SCHEMA_DB_UUID, microschemaVersion.getSchemaContainer().getId());

		HibernateTx.get().getContentInterceptor().persist(micronode);

		if (maybeIndex.isPresent()) {
			insertReferenced(maybeIndex.get(), micronode);
		}
		return micronode;
	}

	@Override
	public Class<? extends MicronodeField> getListType() {
		return HibMicronodeFieldImpl.class;
	}

	@Override
	public Field cloneTo(FieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?,?,?,?,?>) container;
		List<Micronode> values = getValues();
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.LIST);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getMicronodeList, false);
		return HibMicronodeListFieldImpl.fromContainer(tx, unmanagedBase, getFieldKey(), values);
	}

	@Override
	protected HibMicronodeListFieldEdgeImpl makeFromValueAndIndex(Micronode micronode, int index, HibernateTx tx) {
		EntityManager em = tx.entityManager();
		get(index, tx).ifPresent(existing -> {
			tx.forceDelete(existing, "dbUuid", e -> e.getId());
		});
		HibMicronodeListFieldEdgeImpl item = getItemConstructor().provide(tx, valueOrNull(), index, getFieldKey(), micronode, getContainer());
		em.persist(item);
		put(index, item, tx);
		return item;
	}

	@Override
	protected Micronode getValue(MicronodeField field) {
		return field.getMicronode();
	}

	@Override
	protected HibListFieldItemConstructor<HibMicronodeListFieldEdgeImpl, Micronode, UUID> getItemConstructor() {
		return HibMicronodeListFieldEdgeImpl::new;
	}
}
