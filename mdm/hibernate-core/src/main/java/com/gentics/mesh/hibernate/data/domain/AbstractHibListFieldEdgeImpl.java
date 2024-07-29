package com.gentics.mesh.hibernate.data.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.node.field.HibListFieldEdge;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Generic list field item implementation. The default edge implementation is extended with a list index.
 * A set of these items, common for the container UUID, container type, field key, defines the list contents.
 * 
 * @author plyhun
 *
 * @param <U> an item field value type, stored in the database, either primitive value or UUID
 */
@MappedSuperclass
public abstract class AbstractHibListFieldEdgeImpl<U> 
			extends AbstractFieldEdgeImpl<U> implements HibListableField, HibListFieldEdge<U> {

	private static final String QUERY_ITEMS_BY_KEY_CONTENT_TYPE = "select l "
			+ " from %s l "
			+ " where l.containerUuid = :containerUuid "
			+ " and l.fieldKey = :fieldKey "
			+ " and l.containerType = :containerType "
			+ " order by l.itemIndex asc ";

	private static final String QUERY_ITEMS_BY_LISTUUID = "select l "
			+ " from %s l "
			+ " where l.listUuid = :listUuid "
			+ " order by l.itemIndex asc ";

	private static final String QUERY_ITEMS_BY_LISTUUIDS = "select l "
			+ " from %s l "
			+ " where l.listUuid in :listUuids ";

	private static final String QUERY_COUNT_BY_KEY_CONTENT_TYPE = "select count(1) as counts "
			+ " from %s l "
			+ " where l.containerUuid = :containerUuid "
			+ " and l.fieldKey = :fieldKey "
			+ " and l.containerType = :containerType ";
	
	private static final String QUERY_ITEM_BY_KEY_CONTENT_TYPE_INDEX = "select l "
			+ " from %s l "
			+ " where l.containerUuid = :containerUuid "
			+ " and l.fieldKey = :fieldKey "
			+ " and l.containerType = :containerType "
			+ " and l.itemIndex = :index";

	private static final String QUERY_DELETE_BY_KEY_CONTENT_TYPE_INDEX = "delete %s "
			+ " where containerUuid = :containerUuid "
			+ " and fieldKey = :fieldKey "
			+ " and containerType = :containerType "
			+ " and itemIndex = :index";

	private static final String QUERY_DELETE_ALL_BY_KEY_CONTENT_TYPE = "delete %s "
			+ " where containerUuid = :containerUuid "
			+ " and fieldKey = :fieldKey "
			+ " and containerType = :containerType ";

	public static final String QUERY_DELETE_ALL_BY_KEYS = "delete %s where containerUuid in :containerUuids";

	@Column(nullable = false)
	private int itemIndex;
	@Column(nullable = false)
	private UUID listUuid;

	public AbstractHibListFieldEdgeImpl() {
	}

	public AbstractHibListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, U valueOrUuid, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, fieldKey, valueOrUuid, parentFieldContainer);
		this.itemIndex = index;
		this.listUuid = listUuid;
	}

	/**
	 * Make a {@link HibListableField} representation of this item.
	 * 
	 * @return
	 */
	public static <I extends AbstractHibListFieldEdgeImpl<?>> Stream<I> streamItems(
				HibernateTx tx, Class<I> classOfI, UUID containerUuid, ReferenceType containerType, String listFieldKey) {
		return tx.entityManager().createQuery(String.format(QUERY_ITEMS_BY_KEY_CONTENT_TYPE, getEntityTableName(classOfI)), classOfI)
				.setParameter("fieldKey", listFieldKey)
				.setParameter("containerUuid", containerUuid)
				.setParameter("containerType", containerType)
				.getResultStream();
	}

	/**
	 * Get a stream of {@link AbstractHibListFieldEdgeImpl} items for the given listUuid (in the correct order)
	 * @param <I> type of the returned items
	 * @param tx hibernate transaction
	 * @param classOfI class of the returned items
	 * @param listUuid list UUID
	 * @return stream
	 */
	public static <I extends AbstractHibListFieldEdgeImpl<?>> Stream<I> streamItems(HibernateTx tx, Class<I> classOfI,
			UUID listUuid) {
		return tx.entityManager().createQuery(String.format(QUERY_ITEMS_BY_LISTUUID, getEntityTableName(classOfI)), classOfI)
			.setParameter("listUuid", listUuid)
			.getResultStream();
	}

	/**
	 * Get all items for all given lists in the correct order. The returned map will contain all given listUuids as keys.
	 * If there are no items for a specific list UUID, the value will be an empty list.
	 * @param <I> type of the returned items
	 * @param tx hibernate transaction
	 * @param classOfI class of the returned items
	 * @param listUuids list UUIDs
	 * @return map of list UUIDs to lists of items
	 */
	public static <I extends AbstractHibListFieldEdgeImpl<?>> Map<UUID, List<I>> getItems(HibernateTx tx, Class<I> classOfI, Collection<UUID> listUuids) {
		List<I> allItems = SplittingUtils.splitAndMergeInList(listUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> tx.entityManager().createQuery(String.format(QUERY_ITEMS_BY_LISTUUIDS, getEntityTableName(classOfI)), classOfI)
				.setParameter("listUuids", slice)
				.getResultList());
		Map<UUID, List<I>> itemMap = new HashMap<>();

		allItems.forEach(item -> {
			UUID listUuid = item.getListUuid();
			itemMap.computeIfAbsent(listUuid, key -> new ArrayList<>()).add(item);
		});

		listUuids.forEach(listUuid -> {
			Optional.ofNullable(itemMap.get(listUuid)).ifPresent(c -> c.sort((l1, l2) -> Integer.compare(l1.getIndex(), l2.getIndex())));
			itemMap.computeIfAbsent(listUuid, key -> Collections.emptyList());
		});

		return itemMap;
	}

	public static long countItems(
				HibernateTx tx, Class<? extends AbstractHibListFieldEdgeImpl<?>> classOfI, UUID containerUuid, ReferenceType containerType, String listFieldKey) {
		return tx.entityManager().createQuery(String.format(QUERY_COUNT_BY_KEY_CONTENT_TYPE, getEntityTableName(classOfI)), Long.class)
				.setParameter("fieldKey", listFieldKey)
				.setParameter("containerUuid", containerUuid)
				.setParameter("containerType", containerType)
				.getSingleResult().longValue();
	}

	public static <I extends AbstractHibListFieldEdgeImpl<?>> Optional<I> getItem(
				HibernateTx tx, Class<I> classOfI, UUID containerUuid, ReferenceType containerType, String listFieldKey, int index) {
		return tx.entityManager().createQuery(String.format(QUERY_ITEM_BY_KEY_CONTENT_TYPE_INDEX, getEntityTableName(classOfI)), classOfI)
				.setParameter("fieldKey", listFieldKey)
				.setParameter("containerUuid", containerUuid)
				.setParameter("containerType", containerType)
				.setParameter("index", index)
				.getResultStream().findAny();
	}

	public static int deleteItems(
				HibernateTx tx, Class<? extends AbstractHibListFieldEdgeImpl<?>> classOfI, UUID containerUuid, ReferenceType containerType, String listFieldKey) {
		return tx.entityManager().createQuery(String.format(QUERY_DELETE_ALL_BY_KEY_CONTENT_TYPE, getEntityTableName(classOfI)))
				.setParameter("fieldKey", listFieldKey)
				.setParameter("containerUuid", containerUuid)
				.setParameter("containerType", containerType)
				.executeUpdate();
	}
	
	public static int deleteItem(
				HibernateTx tx, Class<? extends AbstractHibListFieldEdgeImpl<?>> classOfI, UUID containerUuid, ReferenceType containerType, String listFieldKey, int index) {
		return tx.entityManager().createQuery(String.format(QUERY_DELETE_BY_KEY_CONTENT_TYPE_INDEX, getEntityTableName(classOfI)))
				.setParameter("fieldKey", listFieldKey)
				.setParameter("containerUuid", containerUuid)
				.setParameter("containerType", containerType)
				.setParameter("index", index)
				.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	public static <I extends AbstractHibListFieldEdgeImpl<?>> int deleteItem(HibernateTx tx, I item) {
		return deleteItem(tx, (Class<? extends AbstractHibListFieldEdgeImpl<?>>) item.getClass(), item.getContainerUuid(), item.getContainerType(), item.getStoredFieldKey(), item.getIndex());
	}

	@Override
	public void setUuid(String uuid) {
		setDbUuid(UUIDUtil.toJavaUuid(uuid));
	}

	@Override
	public String getUuid() {
		return UUIDUtil.toShortUuid(getDbUuid());
	}

	@Override
	public String getFieldKey() {
		return String.valueOf(itemIndex);
	}

	@Override
	public void setFieldKey(String key) {
		setIndex(Integer.valueOf(key));
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		throw new IllegalStateException("Cannot directly clone a list field item");
	}

	public String getFieldName() {
		return super.getFieldKey();
	}

	public int getIndex() {
		return itemIndex;
	}

	public void setIndex(int index) {
		this.itemIndex = index;
	}

	public UUID getListUuid() {
		return listUuid;
	}

	public void setListUuid(UUID listUuid) {
		this.listUuid = listUuid;
	}

	public static String getEntityTableName(Class<? extends AbstractHibListFieldEdgeImpl<?>> classOfItem) {
		return classOfItem.getAnnotation(Entity.class).name();
	}

	@Override
	public String toString() {
		return "AbstractHibListFieldEdgeImpl [index=" + itemIndex + ", listUuid=" + listUuid + ", valueOrUuid="
				+ valueOrUuid + "] " + super.toString();
	}
}
