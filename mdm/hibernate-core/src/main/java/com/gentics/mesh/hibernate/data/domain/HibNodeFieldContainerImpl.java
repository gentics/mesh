package com.gentics.mesh.hibernate.data.domain;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.contentoperation.ContentColumn;
import com.gentics.mesh.contentoperation.DynamicContentColumn;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.EditorTracking;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;
import com.gentics.mesh.hibernate.data.node.field.HibFieldTypes;
import com.gentics.mesh.util.VersionNumber;

/**
 * Content entity.
 * 
 * ATTENTION: despite this class is an entity, it is not managed by the Hibernate Entity manager,
 * as every content schema/table is unique!
 * 
 * @author plyhun
 *
 */
public class HibNodeFieldContainerImpl extends AbstractHibBucketableElement 
		implements HibUnmanagedFieldContainer<SchemaResponse, SchemaVersionModel, SchemaReference, Schema, SchemaVersion>,
			NodeFieldContainer, EditorTracking, Serializable {

	private static final long serialVersionUID = -3107902884955621371L;

	public static final List<ContentColumn> COMMON_COLUMNS = List.of(
			CommonContentColumn.DB_UUID,
			CommonContentColumn.DB_VERSION,
			CommonContentColumn.EDITOR_DB_UUID,
			CommonContentColumn.EDITED,
			CommonContentColumn.BUCKET_ID,
			CommonContentColumn.SCHEMA_DB_UUID,
			CommonContentColumn.SCHEMA_VERSION_DB_UUID,
			CommonContentColumn.LANGUAGE_TAG,
			CommonContentColumn.NODE,
			CommonContentColumn.CURRENT_VERSION_NUMBER);

	private Map<ContentColumn, Object> storage = new HashMap<>();
	
	public HibNodeFieldContainerImpl() {

	}

	@Override
	public Integer getBucketId() {
		return this.<Integer>get(CommonContentColumn.BUCKET_ID, () -> {
			Integer bucketId = contentStorage().findColumn(getSchemaContainerVersion(), getDbUuid(),
					CommonContentColumn.BUCKET_ID);
			if (bucketId != null) {
				return bucketId;
			}
			generateBucketId();
			Integer generatedBucketId = bucketTracking.getBucketId();
			setBucketId(generatedBucketId);
			return generatedBucketId;
		});
	}

	@Override
	public void setBucketId(Integer bucketId) {
		super.setBucketId(bucketId);
		put(CommonContentColumn.BUCKET_ID, bucketId);
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
	public boolean isValid() {
		return contentStorage().findColumn(getSchemaContainerVersion(), getDbUuid(), CommonContentColumn.DB_UUID) != null;
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
	public User getEditor() {
		return this.get(
				CommonContentColumn.EDITOR_DB_UUID, () -> contentStorage().findColumn(getSchemaContainerVersion(),
						getDbUuid(), CommonContentColumn.EDITOR_DB_UUID),
				uuid -> HibernateTx.get().load(uuid, HibUserImpl.class));
	}

	@Override
	public void setEditor(User user) {
		Object editorUuid = (user == null) ? null : user.getId();
		put(CommonContentColumn.EDITOR_DB_UUID, editorUuid);
	}

	@Override
	public Long getLastEditedTimestamp() {
		return get(CommonContentColumn.EDITED,
				() -> contentStorage().findColumn(getSchemaContainerVersion(), getDbUuid(), CommonContentColumn.EDITED),
				Timestamp::getTime);
	}

	@Override
	public void setLastEditedTimestamp(long millis) {
		Timestamp timestamp = new Timestamp(millis);
		put(CommonContentColumn.EDITED, timestamp);
	}

	@Override
	public String getLanguageTag() {
		return this.<String>get(CommonContentColumn.LANGUAGE_TAG, () -> HibUnmanagedFieldContainer.super.getLanguageTag());
	}

	@Override
	public void setLastEditedTimestamp() {
		setLastEditedTimestamp(System.currentTimeMillis());
	}

	@Override
	public String getDisplayFieldValue() {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		return contentDao.getDisplayFieldValue(this);
	}

	@Override
	public Node getNode() {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		return contentDao.getNode(this);
	}

	@Override
	public VersionNumber getVersion() {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		return contentDao.getVersion(this);
	}

	@Override
	public void setVersion(VersionNumber version) {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		contentDao.setVersion(this, version);
	}

	@Override
	public boolean hasNextVersion() {
		ContentDaoImpl contentDao = (ContentDaoImpl) Tx.get().contentDao();
		return contentDao.hasNextVersion(this);
	}

	@Override
	public Iterable<NodeFieldContainer> getNextVersions() {
		ContentDaoImpl contentDao = (ContentDaoImpl) Tx.get().contentDao();
		return contentDao.getNextVersions(this);
	}

	@Override
	public void setNextVersion(NodeFieldContainer container) {
		ContentDaoImpl contentDao = (ContentDaoImpl) Tx.get().contentDao();
		contentDao.setNextVersion(this, container);
	}

	@Override
	public boolean hasPreviousVersion() {
		ContentDaoImpl contentDao = (ContentDaoImpl) Tx.get().contentDao();
		return contentDao.hasPreviousVersion(this);
	}

	@Override
	public NodeFieldContainer getPreviousVersion() {
		ContentDaoImpl contentDao = (ContentDaoImpl) Tx.get().contentDao();
		return contentDao.getPreviousVersion(this);
	}

	@Override
	public void clone(NodeFieldContainer container) {
		ContentDaoImpl contentDao = (ContentDaoImpl) Tx.get().contentDao();
		contentDao.clone(this, container);
	}

	@Override
	public SchemaVersion getSchemaContainerVersion() {
		return HibernateTx.get().load(getSchemaContainerVersionUuid(), HibSchemaVersionImpl.class);
	}

	@Override
	public Object getSchemaContainerVersionUuid() {
		return storage.get(CommonContentColumn.SCHEMA_VERSION_DB_UUID);
	}

	@Override
	public void setSchemaContainerVersion(FieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		put(CommonContentColumn.SCHEMA_VERSION_DB_UUID, version.getId());
		put(CommonContentColumn.SCHEMA_DB_UUID, version.getSchemaContainer().getId());
	}

	@Override
	public ReferenceType getReferenceType() {
		return ReferenceType.FIELD;
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
		return storage.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	@Override
	public void setSchemaContainerVersionByUuid(UUID versionUuid) {
		put(CommonContentColumn.SCHEMA_VERSION_DB_UUID, versionUuid);
	}

	@Override
	public Stream<? extends NodeFieldContainer> getNodeFieldContainers() {
		// this is already a node container we look for
		return Stream.of(this);
	}

	@Override
	public void updateFieldsFromRest(InternalActionContext ac, FieldMap restFields) {
		HibernateTx tx = HibernateTx.get();
		ContentDaoImpl contentDao = tx.contentDao();

		Set<String> urlFieldValuesBeforeUpdate = contentDao.getUrlFieldValues(this).collect(Collectors.toSet());

		HibUnmanagedFieldContainer.super.updateFieldsFromRest(ac, restFields);

		Set<String> urlFieldValuesAfterUpdate = contentDao.getUrlFieldValues(this).collect(Collectors.toSet());
		String branchUuid = tx.getBranch(ac).getUuid();

		// only check for conflicts, if the url field values change
		boolean checkForConflicts = !CollectionUtils.isEqualCollection(urlFieldValuesBeforeUpdate, urlFieldValuesAfterUpdate);
		contentDao.updateWebrootPathInfo(this, ac, branchUuid, "node_conflicting_segmentfield_update", checkForConflicts);
		contentDao.updateDisplayFieldValue(this);
	}

	@Override
	public void createFieldsFromRest(InternalActionContext ac, FieldMap restFields) {
		HibernateTx tx = HibernateTx.get();
		ContentDaoImpl contentDao = tx.contentDao();
		FieldSchemaContainer schema = getSchemaContainerVersion().getSchema();
		schema.assertForUnhandledFields(restFields);

		createBasicFields(schema, restFields);
		createComplexFields(ac, schema, restFields);

		String branchUuid = tx.getBranch(ac).getUuid();
		contentDao.updateWebrootPathInfo(this, ac, branchUuid, "node_conflicting_segmentfield_update");
		contentDao.updateDisplayFieldValue(this);
	}

	/**
	 * Read owner node UUID.
	 * 
	 * @return
	 */
	public UUID getNodeId() {
		return get(CommonContentColumn.NODE, () -> null);
	}

	/**
	 * basic fields can be stored with a single update query
	 * @param schema
	 * @param restFields
	 */
	private void createBasicFields(FieldSchemaContainer schema, FieldMap restFields) {
		List<FieldSchema> basicFields = schema.getFields().stream().filter(this::isBasicField).collect(Collectors.toList());
		for (FieldSchema fieldSchema : basicFields) {
			String key = fieldSchema.getName();
			FieldModel field = restFields.getField(key, fieldSchema);
			boolean restFieldIsNull = field == null || field.getValue() == null;
			if (fieldSchema.isRequired() && restFieldIsNull) {
				throw error(BAD_REQUEST, "node_error_missing_required_field_value", key, schema.getName());
			} else if (restFieldIsNull) {
				continue;
			}

			DynamicContentColumn column = new DynamicContentColumn(fieldSchema);
			put(column, column.transformToPersistedValue(field.getValue()));
		}
	}

	/**
	 * For complex query there is more logic to be observed, so we fall back to the updateField method
	 * @param ac
	 * @param schema
	 * @param restFields
	 */
	private void createComplexFields(InternalActionContext ac, FieldSchemaContainer schema, FieldMap restFields) {
		List<FieldSchema> complexFields = schema.getFields().stream().filter(f -> !isBasicField(f)).collect(Collectors.toList());

		for (FieldSchema entry : complexFields) {
			String key = entry.getName();
			updateField(ac, restFields, key, entry, schema);
		}
	}

	private boolean isBasicField(FieldSchema fieldSchema) {
		HibFieldTypes type = HibFieldTypes.fromFieldSchema(fieldSchema);

		return  HibFieldTypes.STRING.equals(type) ||
				HibFieldTypes.HTML.equals(type) ||
				HibFieldTypes.NUMBER.equals(type) ||
				HibFieldTypes.DATE.equals(type) ||
				HibFieldTypes.BOOLEAN.equals(type);
	}
}
