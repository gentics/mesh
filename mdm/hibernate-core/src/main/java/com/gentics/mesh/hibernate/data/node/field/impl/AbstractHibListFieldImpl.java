package com.gentics.mesh.hibernate.data.node.field.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openjdk.tools.sjavac.Log;
import org.slf4j.Logger;

import com.gentics.mesh.cache.ListableFieldCache;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibListField;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * A special case for the list field implementations, based on {@link AbstractReferenceHibField}.
 * 
 * @author plyhun
 *
 * @param <T> actually referenced list field edge type
 * @param <LF> a list field type, supported by this list container
 * @param <RM> a REST model representation of the <LF>
 * @param <U> an item field value type
 * @param <V> an item field value, actually stored in the database
 */
public abstract class AbstractHibListFieldImpl<
				T extends AbstractHibListFieldEdgeImpl<V>, 
				LF extends HibListableField, RM extends Field, U, V
			> extends AbstractBasicHibField<UUID> implements HibListField<LF, RM, U> {

	public static final Logger log = getLogger(AbstractHibListFieldImpl.class);

	protected final Class<T> itemClass;

	public AbstractHibListFieldImpl(UUID listUuid, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, Class<T> itemClass) {
		super(fieldKey, parent, FieldTypes.LIST, listUuid);
		this.itemClass = itemClass;
		storeValue(value.get());
	}

	public AbstractHibListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, Class<T> itemClass) {
		this(tx.uuidGenerator().generateType1UUID(), fieldKey, parent, itemClass);
	}

	/**
	 * Create an item out of listable field and list index.
	 * 
	 * @param field
	 * @param index
	 * @param tx
	 * @return
	 */
	protected T makeFromListableFieldAndIndex(LF field, int index, HibernateTx tx) {
		return makeFromValueAndIndex(getValue(field), index, tx);
	}

	/**
	 * Create an item out of value and list index.
	 * 
	 * @param value
	 * @param index
	 * @param tx
	 * @return
	 */
	protected T makeFromValueAndIndex(U value, int index, HibernateTx tx) {
		get(index, tx).ifPresent(existing -> {
			tx.forceDelete(existing, "dbUuid", e -> e.getId());
		});
		T item = getItemConstructor().provide(HibernateTx.get(), valueOrNull(), index, getFieldKey(), value, getContainer());
		put(index, item, tx);
		return item;
	}

	/**
	 * Create items out of values.
	 * @param startAt 
	 * 
	 * @param value
	 * @param startAt
	 * @param index
	 * @param tx
	 */
	protected void makeFromValuesAndIndices(List<U> values, int startAt, HibernateTx tx) {
		removeAll(tx);
		List<T> items = IntStream.range(0, values.size()).mapToObj(index -> getItemConstructor().provide(HibernateTx.get(), valueOrNull(), startAt + index, getFieldKey(), values.get(index), getContainer())).collect(Collectors.toList());
		putAll(startAt, items, tx);
	}

	/**
	 * Get a value from the list field.
	 * 
	 * @param field
	 * @return
	 */
	abstract protected U getValue(LF field);

	/**
	 * Get an item constructor.
	 * 
	 * @return
	 */
	abstract protected HibListFieldItemConstructor<T,U,V> getItemConstructor();

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends LF> getList() {
		UUID uuid = valueOrNull();
		HibernateTx tx = HibernateTx.get();
		List<? extends LF> ret = (List<? extends LF>) stream(tx).collect(Collectors.toList());
		ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> cache = tx.data().getListableFieldCache();
		if (cache.get(uuid) == null) {
			cache.put(uuid, (List<? extends AbstractHibListFieldEdgeImpl<?>>) ret);
		}
		return ret;
	}

	/**
	 * This is a backwards compatible override, to prevent duplicates 
	 * for the cases a field has just been added with {@link HibNodeFieldList#createNode(int, HibNode)}.
	 * Avoid using it with the fields created with {@link HibNodeFieldList#createNode(int, HibNode)}
	 */
	@Override
	@Deprecated
	public void addItem(LF field) {
		int index = getSize();
		try {
			index = Integer.parseInt(field.getFieldKey());
		} catch (NumberFormatException e) {
			Log.warn("Invalid field key: " + field.getFieldKey());
		}
		makeFromListableFieldAndIndex(field, index, HibernateTx.get());
	}

	@Override
	public void removeItem(LF field) {
		if (itemClass.isInstance(field)) {
			T item = itemClass.cast(field);
			remove(item.getIndex(), HibernateTx.get());
		}
	}

	@Override
	public int getSize() {
		return (int) size(HibernateTx.get());
	}

	@Override
	public void removeAll() {
		removeAll(HibernateTx.get());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends LF> getListType() {
		return (Class<? extends LF>) itemClass;
	}

	/**
	 * Clean this list.
	 * 
	 * @param tx current transaction
	 */
	public void removeAll(HibernateTx tx) {
		UUID listUuid = valueOrNull();
		int actual = stream(tx).map(edge -> {
			edge.onEdgeDeleted(tx);
			return 1;
		}).mapToInt(Integer::intValue).sum();
		if (actual > 0) {
			int deleted = AbstractHibListFieldEdgeImpl.deleteItems(
					tx, itemClass, getContainer().getDbUuid(), getContainer().getReferenceType(), getFieldKey());
			if (deleted != actual) {
				log.warn("Inconsistency: For [" + itemClass.getSimpleName() + "]/" + listUuid + ": items sizes mismatch: actual " + actual + ", deleted " + deleted);
			}
			ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> cache = tx.data().getListableFieldCache();
			cache.invalidate(listUuid);
		}
	}

	public long size(HibernateTx tx) {
		return AbstractHibListFieldEdgeImpl.countItems(
					tx, itemClass, getContainer().getDbUuid(), getContainer().getReferenceType(), getFieldKey());
	}
	
	public Optional<T> get(int index, HibernateTx tx) {
		return AbstractHibListFieldEdgeImpl.getItem(
					tx, itemClass, getContainer().getDbUuid(), getContainer().getReferenceType(), getFieldKey(), index);
	}

	@SuppressWarnings("unchecked")
	public Stream<T> stream(HibernateTx tx) {
		UUID listUuid = valueOrNull();
		if (listUuid != null) {
			ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> cache = tx.data().getListableFieldCache();
			List<? extends AbstractHibListFieldEdgeImpl<?>> cachedList = cache.get(listUuid);
			if (cachedList != null) {
				return (Stream<T>) cachedList.stream();
			}
			return AbstractHibListFieldEdgeImpl.streamItems(tx, itemClass, listUuid);
		} else {
			return AbstractHibListFieldEdgeImpl.streamItems(
					tx, itemClass, getContainer().getDbUuid(), getContainer().getReferenceType(), getFieldKey());
		}
	}

	public void add(T item, HibernateTx tx) {
		put((int) size(tx), item, tx);
	}

	public void put(int index, T item, HibernateTx tx) {
		item.setIndex(index);
		tx.entityManager().merge(item);

		// clear cache for the list (since we updated it)
		UUID listUuid = valueOrNull();
		if (listUuid != null) {
			ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> cache = tx.data().getListableFieldCache();
			cache.invalidate(listUuid);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void putAll(int startAt, List<T> items, HibernateTx tx) {
		if (items.size() > 0) {
			EntityManager em = tx.entityManager();
			// since we add 8 parameters for each item, we need to divide the overall parameter limit by 8
			long inserted = SplittingUtils.splitAndCount(items, HibernateUtil.inQueriesLimitForSplitting(0) / 8, (base, slice) -> {
				Query query = tx.data().getDatabaseConnector().makeListItemsMultiInsertionQuery(slice, em, startAt + base);
				return Long.valueOf(query.executeUpdate());
			});
			if (inserted != items.size()) {
				throw new IllegalStateException("Items sizes mismatch: actual " + items.size() + ", inserted " + inserted);
			}
		}

		// clear cache for the list (since we updated it)
		UUID listUuid = valueOrNull();
		if (listUuid != null) {
			ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> cache = tx.data().getListableFieldCache();
			cache.invalidate(listUuid);
		}
	}

	public boolean remove(int index, HibernateTx tx) {
		UUID listUuid = valueOrNull();
		boolean ret = AbstractHibListFieldEdgeImpl.deleteItem(
				tx, itemClass, getContainer().getDbUuid(), getContainer().getReferenceType(), getFieldKey(), index) == 1;
		if (ret && listUuid != null) {
			ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> cache = tx.data().getListableFieldCache();
			cache.invalidate(listUuid);
		}
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		return listEquals(obj);
	}

	@Override
	public void onFieldDeleted(HibernateTx tx) {
		removeAll(tx);
	}

	@Override
	public void setUuid(String uuid) {
		throw new IllegalStateException("Cannot set a UUID for list");
	}

	@Override
	public String getUuid() {
		return getId().toString();
	}

	@Override
	public Object getId() {
		return valueOrNull();
	}

	@Override
	public String getElementVersion() {
		return String.valueOf(0);
	}

	@FunctionalInterface
	public interface HibListFieldItemConstructor<T extends AbstractHibListFieldEdgeImpl<V>, U, V> {
		T provide(HibernateTx tx, UUID listUuid, int index, String fieldKey, U valueOrUuid, HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer);
	}
}
