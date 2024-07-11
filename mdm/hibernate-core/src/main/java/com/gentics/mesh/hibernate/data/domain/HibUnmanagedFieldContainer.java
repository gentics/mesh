package com.gentics.mesh.hibernate.data.domain;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.contentoperation.ContentColumn;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.contentoperation.DynamicContentColumn;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryField;
import com.gentics.mesh.core.data.node.field.BooleanField;
import com.gentics.mesh.core.data.node.field.DateField;
import com.gentics.mesh.core.data.node.field.HtmlField;
import com.gentics.mesh.core.data.node.field.NumberField;
import com.gentics.mesh.core.data.node.field.StringField;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3BinaryField;
import com.gentics.mesh.core.data.schema.FieldSchemaElement;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.ContentInterceptor;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.node.field.HibFieldTypes;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractBasicHibField;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractBasicHibField.BasicHibFieldConstructor;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractHibField;
import com.gentics.mesh.hibernate.data.node.field.impl.HibBinaryFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibBooleanFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibBooleanListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibDateFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibDateListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibHtmlFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibHtmlListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibMicronodeFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibMicronodeListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibNodeFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibNodeListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibNumberFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibNumberListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibS3BinaryFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibStringFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibStringListFieldImpl;
import com.gentics.mesh.util.UUIDUtil;

import org.slf4j.Logger;

/**
 * A base for the field container entities, that are not known to the Hibernate entity manager.
 * 
 * @author plyhun
 *
 * @param <R> REST entity POJO type
 * @param <RM> REST model type
 * @param <RE> Reference type
 * @param <SC> container schema type
 * @param <SCV> container schema version type
 */
public interface HibUnmanagedFieldContainer<
		R extends FieldSchemaContainer, 
		RM extends FieldSchemaContainerVersion, 
		RE extends NameUuidReference<RE>, 
		SC extends FieldSchemaElement<R, RM, RE, SC, SCV>, 
		SCV extends FieldSchemaVersionElement<R, RM, RE, SC, SCV>> extends HibFieldContainerBase {
	
	public static final Logger log = getLogger(HibUnmanagedFieldContainer.class);

	/**
	 * Get the node that this container belongs to.
	 * 
	 * @return
	 */
	Node getNode();

	/**
	 * Find and set the the schema version by given UUID.
	 * 
	 * @param versionUuid
	 * @return
	 */
	void setSchemaContainerVersionByUuid(UUID versionUuid);

	/**
	 * Get the schema version.
	 */
	@Override
	SCV getSchemaContainerVersion();

	/**
	 * Get the schema version UUID object;
	 * 
	 * @return
	 */
	Object getSchemaContainerVersionUuid();

	/**
	 * Get the stored value for the column.
	 * If the value is not yet stored in the storage, use the supplier to fetch the value
	 * and store it in the storage.
	 * 
	 * @param <T> value type
	 * @param column column
	 * @param supplier supplier to fetch non-existing values
	 * @return value
	 */
	<T> T get(ContentColumn column, Supplier<T> supplier);

	/**
	 * Variant of {@link #get(ContentColumn, Supplier)} with an additional mapper to map non-null
	 * values to other types
	 * 
	 * @param <T> type of the stored value
	 * @param <U> return type
	 * @param column column
	 * @param supplier supplier to fetch non-existing values
	 * @param mapper mapper for mapping non-null values
	 * @return mapped value
	 */
	default <T, U> U get(ContentColumn column, Supplier<T> supplier, Function<T, U> mapper) {
		T value = get(column, supplier);
		if (value != null) {
			return mapper.apply(value);
		} else {
			return null;
		}
	}

	/**
	 * Store the column value in the storage.
	 * 
	 * @param <T>
	 * @param column
	 * @param value
	 */
	<T> void put(ContentColumn column, T value);

	/**
	 * Remove the column value from the storage
	 *
	 * @param column
	 */
	void remove(ContentColumn column);

	/**
	 * Get all stored values
	 * @return list of pairs consisting of the ContentColumn and the value
	 */
	List<Pair<ContentColumn, Object>> getAll();

	/**
	 * REST <--> Entity field updater.
	 * 
	 * @param ac context
	 * @param restFields input REST field
	 * @param key field key
	 * @param entry field schema
	 * @param schema field container
	 */
	default void updateField(InternalActionContext ac, FieldMap restFields, String key, FieldSchema entry, FieldSchemaContainer schema) {
		HibFieldTypes type = HibFieldTypes.fromFieldSchema(entry);
		if (type != null) {
			type.updateField(this, ac, restFields, key, entry, schema);
		} else {
			throw error(BAD_REQUEST, "type unknown");
		}
	}

	@Override
	default String getDatabaseEntityName() {
		return MeshTablePrefixStrategy.CONTENT_TABLE_NAME_PREFIX + UUIDUtil.toShortUuid(getSchemaContainerVersionUuid().toString());
	}

	@Override
	default void storeValue(AbstractBasicHibField<?> field, Object value) {
		DynamicContentColumn column = new DynamicContentColumn(getFieldSchema(field.getFieldKey()));
		put(column, value);
	}

	@Override
	default void updateFieldsFromRest(InternalActionContext ac, FieldMap restFields) {
		FieldSchemaContainer schema = getSchemaContainerVersion().getSchema();
		schema.assertForUnhandledFields(restFields);

		// Iterate over all known field that are listed in the schema for the node
		for (FieldSchema entry : schema.getFields()) {
			String key = entry.getName();
			updateField(ac, restFields, key, entry, schema);
		}
	}

	@Override
	default Field getField(FieldSchema fieldSchema) {
		HibFieldTypes type = HibFieldTypes.fromFieldSchema(fieldSchema);
		if (type != null) {
			return type.getField(this, fieldSchema);
		} else {
			throw new GenericRestException(INTERNAL_SERVER_ERROR, "Unknown list type {" + fieldSchema.getType() + "}");
		}
	}

	@Override
	default StringField getString(String key) {
		return getFieldValueFromNullableColumn(key, FieldTypes.STRING, HibStringFieldImpl::new);
	}

	@Override
	default HibBinaryFieldImpl getBinary(String key) {
		return getReferenceFieldValueFromNullableColumn(key, FieldTypes.BINARY, HibBinaryFieldImpl::new);
	}

	@Override
	default String getBinaryFileName(String key) {
		HibBinaryFieldImpl binary = getBinary(key);
		return binary != null ? binary.getFileName() : null;
	}

	@Override
	default BinaryField createBinary(String key, Binary binary) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.BINARY);
		ensureOldReferenceRemoved(tx, key, this::getBinary, false);

		HibBinaryImpl hibBinary = (HibBinaryImpl) binary;
		HibBinaryFieldEdgeImpl edge = HibBinaryFieldEdgeImpl.fromContainer(tx, this, key, hibBinary);
		tx.entityManager().persist(edge);
		return new HibBinaryFieldImpl(this, edge);
	}

	@Override
	default HibS3BinaryFieldImpl getS3Binary(String key) {
		return getReferenceFieldValueFromNullableColumn(key, FieldTypes.S3BINARY, HibS3BinaryFieldImpl::new);
	}

	@Override
	default String getS3BinaryFileName(String key) {
		HibS3BinaryFieldImpl s3Binary = getS3Binary(key);
		return s3Binary != null ? s3Binary.getFileName() : null;
	}

	@Override
	default S3BinaryField createS3Binary(String key, S3Binary binary) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.S3BINARY);
		ensureOldReferenceRemoved(tx, key, this::getS3Binary, false);

		HibS3BinaryImpl hibS3Binary = (HibS3BinaryImpl) binary;
		HibS3BinaryFieldEdgeImpl edge = HibS3BinaryFieldEdgeImpl.fromContainer(tx, this, key, hibS3Binary);
		tx.entityManager().persist(edge);
		return new HibS3BinaryFieldImpl(this, edge);
	}

	@Override
	default HibNodeFieldImpl getNode(String key) {
		return getReferenceFieldValueFromNullableColumn(key, FieldTypes.NODE, HibNodeFieldImpl::new);
	}

	@Override
	default NodeField createNode(String key, Node node) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.NODE);
		ensureOldReferenceRemoved(tx, key, this::getNode, false);

		HibNodeImpl hibNode = (HibNodeImpl) node;
		HibNodeFieldEdgeImpl edge = HibNodeFieldEdgeImpl.fromContainer(tx, this, key, hibNode);
		tx.entityManager().persist(edge);
		return new HibNodeFieldImpl(this, edge);
	}

	@Override
	default DateField getDate(String key) {
		return getFieldValueFromNullableColumn(key, FieldTypes.DATE, HibDateFieldImpl::new);
	}

	@Override
	default NumberField getNumber(String key) {
		return getFieldValueFromNullableColumn(key, FieldTypes.NUMBER, HibNumberFieldImpl::new);
	}

	@Override
	default HtmlField getHtml(String key) {
		return getFieldValueFromNullableColumn(key, FieldTypes.HTML, HibHtmlFieldImpl::new);
	}

	@Override
	default BooleanField getBoolean(String key) {
		return getFieldValueFromNullableColumn(key, FieldTypes.BOOLEAN, HibBooleanFieldImpl::new);
	}

	@Override
	default BooleanField createBoolean(String key) {
		ensureColumnExists(key, FieldTypes.BOOLEAN);
		return new HibBooleanFieldImpl(key, this, null);
	}

	@Override
	default StringField createString(String key) {
		ensureColumnExists(key, FieldTypes.STRING);
		return new HibStringFieldImpl(key, this, null);
	}

	@Override
	default NumberField createNumber(String key) {
		ensureColumnExists(key, FieldTypes.NUMBER);
		return new HibNumberFieldImpl(key, this, null);

	}

	@Override
	default DateField createDate(String key) {
		ensureColumnExists(key, FieldTypes.DATE);
		return new HibDateFieldImpl(key, this, (Instant) null);
	}

	@Override
	default HtmlField createHTML(String key) {
		ensureColumnExists(key, FieldTypes.HTML);
		return new HibHtmlFieldImpl(key, this, null);
	}
	
	@Override
	default HibMicronodeFieldImpl getMicronode(String key) {
		return getReferenceFieldValueFromNullableColumn(key, FieldTypes.MICRONODE, HibMicronodeFieldImpl::new);
	}

	@Override
	default MicronodeField createMicronode(String key, MicroschemaVersion microschemaVersion) {
		HibernateTx tx = HibernateTx.get();

		// 1. Copy existing micronode
		HibMicronodeFieldImpl existing = getMicronode(key);
		Micronode existingMicronode = null;
		if (existing != null) {
			existingMicronode = existing.getMicronode();
			// existing.getMicronode().delete();
		}

		ensureColumnExists(key, FieldTypes.MICRONODE);

		// 2. Create a new micronode and assign the given schema to it
		HibMicronodeContainerImpl micronode = new HibMicronodeContainerImpl();
		micronode.setDbUuid(tx.uuidGenerator().generateType1UUID());
		micronode.setSchemaContainerVersion(microschemaVersion);
		micronode.put(CommonContentColumn.DB_VERSION, 1L);
		micronode.put(CommonContentColumn.SCHEMA_VERSION_DB_UUID, microschemaVersion.getId());
		micronode.put(CommonContentColumn.SCHEMA_DB_UUID, microschemaVersion.getSchemaContainer().getId());

		if (existingMicronode != null) {
			micronode.clone(existingMicronode);
		}

		HibernateTx.get().getContentInterceptor().persist(micronode);

		ensureOldReferenceRemoved(tx, key, this::getMicronode, false);

		// 3. Create a new edge from the container to the created micronode field
		HibMicronodeFieldEdgeImpl edge = HibMicronodeFieldEdgeImpl.fromContainer(tx, this, key, micronode);
		tx.entityManager().persist(edge);

		return new HibMicronodeFieldImpl(this, edge);
	}

	@Override
	default MicronodeField createEmptyMicronode(String key, MicroschemaVersion microschemaVersion) {
		HibernateTx tx = HibernateTx.get();

		ensureColumnExists(key, FieldTypes.MICRONODE);

		// 1. Create a new micronode and assign the given schema to it
		HibMicronodeContainerImpl micronode = new HibMicronodeContainerImpl();
		micronode.setDbUuid(tx.uuidGenerator().generateType1UUID());
		micronode.setSchemaContainerVersion(microschemaVersion);
		micronode.put(CommonContentColumn.DB_VERSION, 1L);
		micronode.put(CommonContentColumn.SCHEMA_VERSION_DB_UUID, microschemaVersion.getId());
		micronode.put(CommonContentColumn.SCHEMA_DB_UUID, microschemaVersion.getSchemaContainer().getId());

		ContentInterceptor contentInterceptor = HibernateTx.get().getContentInterceptor();
		contentInterceptor.persist(micronode);

		// 2. Create a new edge from the container to the created micronode field
		HibMicronodeFieldEdgeImpl edge = HibMicronodeFieldEdgeImpl.fromContainer(tx, this, key, micronode);
		tx.entityManager().persist(edge);

		return new HibMicronodeFieldImpl(this, edge);
	}

	@Override
	default HibDateListFieldImpl getDateList(String key) {
		return getListFieldFromNullableColumn(key, FieldTypes.DATE, HibDateListFieldImpl::new);
	}

	@Override
	default DateFieldList createDateList(String key) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.LIST);
		ensureOldReferenceRemoved(tx, key, this::getDateList, false);
		return HibDateListFieldImpl.fromContainer(tx, this, key, Collections.emptyList());
	}

	@Override
	default HibHtmlListFieldImpl getHTMLList(String key) {
		return getListFieldFromNullableColumn(key, FieldTypes.HTML, HibHtmlListFieldImpl::new);
	}

	@Override
	default HtmlFieldList createHTMLList(String key) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.LIST);
		ensureOldReferenceRemoved(tx, key, this::getHTMLList, false);
		return HibHtmlListFieldImpl.fromContainer(tx, this, key, Collections.emptyList());
	}

	@Override
	default HibNumberListFieldImpl getNumberList(String key) {
		return getListFieldFromNullableColumn(key, FieldTypes.NUMBER, HibNumberListFieldImpl::new);
	}

	@Override
	default NumberFieldList createNumberList(String key) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.LIST);
		ensureOldReferenceRemoved(tx, key, this::getNumberList, false);
		return HibNumberListFieldImpl.fromContainer(tx, this, key, Collections.emptyList());
	}

	@Override
	default HibNodeListFieldImpl getNodeList(String key) {
		return this.<UUID, HibNodeListFieldImpl>getListFieldFromNullableColumn(key, FieldTypes.NODE, HibNodeListFieldImpl::new);
	}

	@Override
	default NodeFieldList createNodeList(String key) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.LIST);
		ensureOldReferenceRemoved(tx, key, this::getNodeList, false);
		return HibNodeListFieldImpl.fromContainer(tx, this, key, Collections.emptyList());
	}

	@Override
	default HibStringListFieldImpl getStringList(String key) {
		return getListFieldFromNullableColumn(key, FieldTypes.STRING, HibStringListFieldImpl::new);
	}

	@Override
	default StringFieldList createStringList(String key) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.LIST);
		ensureOldReferenceRemoved(tx, key, this::getStringList, false);
		return HibStringListFieldImpl.fromContainer(tx, this, key, Collections.emptyList());
	}

	@Override
	default HibBooleanListFieldImpl getBooleanList(String key) {
		return getListFieldFromNullableColumn(key, FieldTypes.BOOLEAN, HibBooleanListFieldImpl::new);
	}

	@Override
	default BooleanFieldList createBooleanList(String key) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.LIST);
		ensureOldReferenceRemoved(tx, key, this::getBooleanList, false);
		return HibBooleanListFieldImpl.fromContainer(tx, this, key, Collections.emptyList());
	}

	@Override
	default HibMicronodeListFieldImpl getMicronodeList(String key) {
		return this.<UUID, HibMicronodeListFieldImpl>getListFieldFromNullableColumn(key, FieldTypes.MICRONODE, HibMicronodeListFieldImpl::new);
	}

	@Override
	default MicronodeFieldList createMicronodeList(String key) {
		HibernateTx tx = HibernateTx.get();
		ensureColumnExists(key, FieldTypes.LIST);
		ensureOldReferenceRemoved(tx, key, this::getMicronodeList, false);
		return HibMicronodeListFieldImpl.fromContainer(tx, this, key, Collections.emptyList());
	}

	@Override
	default void removeField(String fieldKey, BulkActionContext bac) {
		AbstractBasicHibField<?> field = (AbstractBasicHibField<?>) getField(fieldKey);
		if (field == null) {
			return;
		}
		field.onFieldDeleted(HibernateTx.get(), bac);
		DynamicContentColumn column = new DynamicContentColumn(getFieldSchema(fieldKey));
		remove(column);
	}

	private <FIELD extends AbstractBasicHibField<UUID>> 
			FIELD getReferenceFieldValueFromNullableColumn(String key, FieldTypes type, BasicHibFieldConstructor<UUID, FIELD> fieldConstructor) {
		return this.<UUID, FIELD>getFieldValueFromNullableColumn(key, type, fieldConstructor);
	}

	private <V, FIELD extends AbstractBasicHibField<V>> FIELD getFieldValueFromNullableColumn(String key, FieldTypes type, BasicHibFieldConstructor<V, FIELD> fieldConstructor) {
		V value = getFieldValue(() -> {
			FieldSchema field = getFieldSchema(key);
			if (field == null) {
				return null;
			}
			// make sure that the field schema matches the required type
			FieldTypes fetchedType = FieldTypes.valueByName(field.getType());
			if (!Objects.equals(type, fetchedType)) {
				return null;
			}

			return field;
		});

		if (value == null) {
			return null;
		}
		return fieldConstructor.create(key, this, value);
	}

	private <V, FIELD extends AbstractBasicHibField<V>> FIELD getListFieldFromNullableColumn(String key, FieldTypes listType, BasicHibFieldConstructor<V, FIELD> fieldConstructor) {
		V value = getFieldValue(() -> {
			FieldSchema field = getFieldSchema(key);
			if (!(field instanceof ListFieldSchema)) {
				return null;
			}
			// make sure that the list item type matches the required type
			FieldTypes type = FieldTypes.valueByName(((ListFieldSchema) field).getListType());
			if (!Objects.equals(type, listType)) {
				return null;
			}

			return field;
		});
		if (value == null) {
			return null;
		}
		return fieldConstructor.create(key, this, value);
	}

	private <T> T getFieldValue(Supplier<FieldSchema> fieldSchemaSupplier) {
		FieldSchema field = fieldSchemaSupplier.get();
		if (field == null) {
			return null;
		}
		return this.<T>get(new DynamicContentColumn(field), () -> null);
	}

	default void ensureColumnExists(String key, FieldTypes type) {
		FieldSchema field = getSchemaContainerVersion().getSchema().getField(key);
		if (field == null) {
			log.error("ERROR: The field { " + key + " } does not exist for the schema { " + getSchemaContainerVersion().getName() 
					+ " } version { " + getSchemaContainerVersion().getName() + " }:\n" + getSchemaContainerVersion().getJson());
		} else if (!FieldTypes.valueByName(field.getType()).equals(type)) {
			log.error("ERROR: The field { " + key + " } of the schema { " + getSchemaContainerVersion().getName() 
					+ " } version { " + getSchemaContainerVersion().getName() 
					+ " } does not match the requested type { " + type + " }:\n" + getSchemaContainerVersion().getJson());
			}
		}

	default <V, T extends AbstractBasicHibField<V>> void ensureOldReferenceRemoved(HibernateTx tx, String fieldKey, Function<String, T> fieldGetter, boolean throwOnExisting) {
		AbstractBasicHibField<?> field = fieldGetter.apply(fieldKey);
		if (field != null) {
			if (throwOnExisting) {
				throw new RuntimeException(
						"Value exists at { mesh_content_" + getSchemaContainerVersion().getUuid() + "/" + getUuid() + "/" + fieldKey + " }: " + field.toString());
			} else {
				// Cleanup the reference field
				field.onFieldDeleted(tx, new DummyBulkActionContext());
			}
		}
	}

	@Override
	default String getLanguageTag() {
		return contentStorage().findColumn(getSchemaContainerVersion(), getDbUuid(), CommonContentColumn.LANGUAGE_TAG);
	}

	@Override
	default void setLanguageTag(String languageTag) {
		put(CommonContentColumn.LANGUAGE_TAG, languageTag);
	}

	@Override
	default FieldModel getRestField(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema,
			List<String> languageTags, int level) {
		HibFieldTypes type = HibFieldTypes.fromFieldSchema(fieldSchema);
		if (type != null) {
			return type.getRestField(this, ac, fieldKey, fieldSchema, languageTags, level, this::getNode);
		} else {
			throw error(BAD_REQUEST, "type unknown");
		}
	}

	@Override
	default void removeField(Field field) {
		if (field != null) {
			String fieldKey = field.getFieldKey();
			Field existing = getField(fieldKey);
			if (existing != null) {
				if (existing.equals(field)) {
					removeField(fieldKey);
				} else {
					HibernateTx tx = HibernateTx.get();
					PersistingContentDao contentDao = tx.contentDao();
					field = contentDao.detachField(field);
					existing = contentDao.detachField(existing);
					if (field == existing || field == null) {
						removeField(fieldKey);
					} else {
						if (AbstractHibField.class.isInstance(field)) {
							AbstractHibField.class.cast(field).onFieldDeleted(tx, new DummyBulkActionContext());
						} else if (HibFieldEdge.class.isInstance(field)) {
							HibFieldEdge.class.cast(field).onEdgeDeleted(tx, new DummyBulkActionContext());
						}
						if (HibDatabaseElement.class.isInstance(field)) {
							tx.delete(HibDatabaseElement.class.cast(field));
						}
					}
				}
			} else {
				removeField(fieldKey);
			}
		}
	}

	/**
	 * get the content storage instance
	 * @return
	 */
	default ContentStorage contentStorage() {
		return HibernateTx.get().data().getContentStorage();
	}
}
