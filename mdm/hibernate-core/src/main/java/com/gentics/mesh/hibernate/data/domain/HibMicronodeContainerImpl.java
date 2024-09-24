package com.gentics.mesh.hibernate.data.domain;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.contentoperation.ContentColumn;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.database.HibernateTx;

/**
 * Micronode entity.
 * 
 * ATTENTION: despite this class is an entity, it is not managed by the Hibernate Entity manager,
 * as every micronode schema/table is unique!
 * 
 * @author plyhun
 *
 */
public class HibMicronodeContainerImpl extends AbstractHibBaseElement 
		implements HibUnmanagedFieldContainer<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion>, 
		HibMicronode, Serializable {

	private static final long serialVersionUID = 1420762090369895613L;

	public static final Set<ContentColumn> COMMON_COLUMNS = Set.of(
			CommonContentColumn.DB_UUID,
			CommonContentColumn.DB_VERSION,
			CommonContentColumn.SCHEMA_DB_UUID,
			CommonContentColumn.SCHEMA_VERSION_DB_UUID);

	private Map<ContentColumn, Object> storage = new HashMap<>();

	public HibMicronodeContainerImpl() {

	}

	@Override
	public void setDbUuid(UUID uuid) {
		super.setDbUuid(uuid);
		put(CommonContentColumn.DB_UUID, uuid);
	}

	@Override
	public void setDbVersion(Long version) {
		super.setDbVersion(version);
		put(CommonContentColumn.DB_VERSION, version);
	}

	@Override
	public void setSchemaContainerVersion(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		put(CommonContentColumn.SCHEMA_VERSION_DB_UUID, version.getId());
		put(CommonContentColumn.SCHEMA_DB_UUID, version.getSchemaContainer().getId());
	}

	@Override
	public ReferenceType getReferenceType() {
		return HibMicronode.super.getReferenceType();
	}

	@Override
	public HibNodeFieldContainer getContainer() {
		return getContents().findAny().orElseThrow();
	}

	@Override
	public Result<HibNodeFieldContainerImpl> getContainers(boolean lookupInFields, boolean lookupInLists) {
		Stream<HibNodeFieldContainerImpl> nodes = getContainerEdges()
				.filter(edge -> (lookupInFields && edge instanceof HibMicronodeFieldEdgeImpl) || (lookupInLists && edge instanceof HibMicronodeListFieldEdgeImpl))
				.flatMap(edge -> edge.getReferencingContents(true, true));
		return new TraversalResult<>(nodes);
	}

	@Override
	public HibMicroschemaVersion getSchemaContainerVersion() {
		return HibernateTx.get().load(getSchemaContainerVersionUuid(), HibMicroschemaVersionImpl.class);
	}

	@Override
	public Object getSchemaContainerVersionUuid() {
		return storage.get(CommonContentColumn.SCHEMA_VERSION_DB_UUID);
	}
	
	@Override
	public String getLanguageTag() {
		// micronodes do not have a language
		return null;
	}

	@Override
	public HibNode getNode() {
		ContentDao contentDao = Tx.get().contentDao();
		Optional<? extends HibNodeFieldContainer> container = getContents().findAny();
		if (container.isEmpty()) {
			return null;
		}
		HibNodeFieldContainer tmp = container.get();
		while (tmp.getPreviousVersion() != null) {
			tmp = tmp.getPreviousVersion();
		}
		return contentDao.getNode(tmp);
	}

	@Override
	public boolean equals(Object obj) {
		return micronodeEquals(obj);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(ContentColumn column, Supplier<T> supplier) {
		if (!storage.containsKey(column)) {
			storage.put(column, supplier.get());
		}
		return (T) storage.get(column);
	}

	@Override
	public <T> void put(ContentColumn column, T value) {
		storage.put(column, value);
	}

	@Override
	public void remove(ContentColumn column) {
		storage.remove(column);
	}

	@Override
	public List<Pair<ContentColumn, Object>> getAll() {
		return storage.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).collect(Collectors.toList());
	}

	@Override
	public void setSchemaContainerVersionByUuid(UUID versionUuid) {
		put(CommonContentColumn.SCHEMA_VERSION_DB_UUID, versionUuid);
	}

	@Override
	public UUID getDbUuid() {
		return this.<UUID>get(CommonContentColumn.DB_UUID, super::getDbUuid);
	}

	@Override
	public Long getDbVersion() {
		return this.<Long>get(CommonContentColumn.DB_VERSION, super::getDbVersion);
	}

	@Override
	public Field getRestField(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, java.util.List<String> languageTags,
		int level) {

		// Filter out unsupported field types
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case BINARY:
		case MICRONODE:
			throw error(BAD_REQUEST, "error_unsupported_fieldtype", type.name());
		case LIST:
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			switch (listFieldSchema.getListType()) {
			case HibMicronodeFieldList.TYPE:
				throw error(BAD_REQUEST, "error_unsupported_fieldtype", type + ":" + listFieldSchema.getListType());
			default:
				return HibUnmanagedFieldContainer.super.getRestField(ac, fieldKey, fieldSchema, languageTags, level);
			}
		default:
			return HibUnmanagedFieldContainer.super.getRestField(ac, fieldKey, fieldSchema, languageTags, level);
		}
	}

	@Override
	public void updateField(InternalActionContext ac, FieldMap fieldMap, String key, FieldSchema fieldSchema, FieldSchemaContainer schema) {

		// Filter out unsupported field types
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case BINARY:
		case MICRONODE:
			throw error(BAD_REQUEST, "error_unsupported_fieldtype", type.name());
		case LIST:
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			switch (listFieldSchema.getListType()) {
			case HibMicronodeFieldList.TYPE:
				throw error(BAD_REQUEST, "error_unsupported_fieldtype", type + ":" + listFieldSchema.getListType());
			default:
				HibUnmanagedFieldContainer.super.updateField(ac, fieldMap, key, fieldSchema, schema);
			}
		default:
			HibUnmanagedFieldContainer.super.updateField(ac, fieldMap, key, fieldSchema, schema);
		}
	}

	@Override
	public Stream<? extends HibNodeFieldContainer> getNodeFieldContainers() {
		return getContainers().stream();
	}

	public Stream<? extends AbstractFieldEdgeImpl<UUID>> getContainerEdges() {
		EntityManager em = HibernateTx.get().entityManager();
		return Stream.concat(
				em.createNamedQuery("micronodefieldref.findEdgeByMicronodeUuid", HibMicronodeFieldEdgeImpl.class)
					.setParameter("uuid", getDbUuid())
					.getResultStream(),
				em.createNamedQuery("micronodelistitem.findByMicronodeUuid", HibMicronodeListFieldEdgeImpl.class)
					.setParameter("micronodeUuid", getDbUuid())
					.getResultStream()
			);
	}
}
