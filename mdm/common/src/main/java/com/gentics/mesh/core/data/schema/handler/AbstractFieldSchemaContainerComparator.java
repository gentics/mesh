package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.EMPTY;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.rest.schema.BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.util.CompareUtils;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract implementation for a field schema comparator.
 * 
 * @param <FC>
 *            Actual field schema container type
 */
public abstract class AbstractFieldSchemaContainerComparator<FC extends FieldSchemaContainer> implements FieldSchemaContainerComparator<FC> {

	private static final Logger log = LoggerFactory.getLogger(AbstractFieldSchemaContainerComparator.class);

	protected FieldSchemaComparator fieldComparator = new FieldSchemaComparator();

	@Override
	public abstract List<SchemaChangeModel> diff(FC containerA, FC containerB);

	/**
	 * Create a diff of both provides field containers.
	 * 
	 * @param containerA
	 *            Current container
	 * @param containerB
	 *            New container
	 * @param classOfFC
	 *            Implementation class of the field containers
	 * @return
	 */
	protected List<SchemaChangeModel> diff(FC containerA, FC containerB, Class<? extends FC> classOfFC) {
		Objects.requireNonNull(containerA, "containerA must not be null");
		Objects.requireNonNull(containerB, "containerB must not be null");

		containerA.validate();
		containerB.validate();
		List<SchemaChangeModel> changes = new ArrayList<>();

		// Diff the fields
		Map<String, FieldSchema> schemaAFields = transformFieldsToMap(containerA);
		Map<String, FieldSchema> schemaBFields = transformFieldsToMap(containerB);

		for (FieldSchema fieldInA : containerA.getFields()) {
			// Check whether the field was removed in schemaB
			boolean wasRemoved = !schemaBFields.containsKey(fieldInA.getName());
			if (wasRemoved) {
				if (log.isDebugEnabled()) {
					log.debug("Field " + fieldInA.getName() + " was removed.");
				}
				SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange(fieldInA.getName());
				changes.add(change);
			}
		}

		for (int i = 0; i < containerB.getFields().size(); i++) {
			FieldSchema fieldInB = containerB.getFields().get(i);
			FieldSchema fieldInA = schemaAFields.get(fieldInB.getName());

			// Check whether the field was added in schemaB
			if (fieldInA == null) {
				if (log.isDebugEnabled()) {
					log.debug("Field " + fieldInB.getName() + " was added.");
				}
				SchemaChangeModel change = SchemaChangeModel.createAddFieldChange(fieldInB.getName(), fieldInB.getType(), fieldInB.getLabel(), fieldInB.getElasticsearch());
				if (fieldInB instanceof ListFieldSchema) {
					ListFieldSchema listFieldInB = (ListFieldSchema) fieldInB;
					change.setProperty(SchemaChangeModel.LIST_TYPE_KEY, listFieldInB.getListType());
					change.setProperty(SchemaChangeModel.ALLOW_KEY, listFieldInB.getAllowedSchemas());
				}
				if (fieldInB instanceof MicronodeFieldSchema) {
					change.setProperty(SchemaChangeModel.ALLOW_KEY, ((MicronodeFieldSchema) fieldInB).getAllowedMicroSchemas());
				}
				if (fieldInB instanceof NodeFieldSchema) {
					change.setProperty(SchemaChangeModel.ALLOW_KEY, ((NodeFieldSchema) fieldInB).getAllowedSchemas());
				}
				if (fieldInB instanceof StringFieldSchema) {
					change.setProperty(SchemaChangeModel.ALLOW_KEY, ((StringFieldSchema) fieldInB).getAllowedValues());
				}
				if (fieldInB instanceof BinaryFieldSchema) {
					BinaryFieldSchema field = (BinaryFieldSchema) fieldInB;
					BinaryExtractOptions options = field.getBinaryExtractOptions();
					if (options != null) {
						change.setProperty(BinaryFieldSchemaImpl.CHANGE_EXTRACT_CONTENT_KEY, options.getContent());
						change.setProperty(BinaryFieldSchemaImpl.CHANGE_EXTRACT_METADATA_KEY, options.getMetadata());
					}
				}

				change.setProperty(SchemaChangeModel.REQUIRED_KEY, fieldInB.isRequired());
				change.setProperty(SchemaChangeModel.NO_INDEX_KEY, fieldInB.isNoIndex());

				if (i - 1 >= 0) {
					FieldSchema fieldBefore = containerB.getFields().get(i - 1);
					if (fieldBefore != null) {
						change.setProperty(SchemaChangeModel.ADD_FIELD_AFTER_KEY, fieldBefore.getName());
					}
				}
				changes.add(change);
			} else {
				// Field was not added or removed. It exists in both schemas. Lets see whether it changed
				SchemaChangeModel change = fieldComparator.compare(fieldInA, fieldInB);
				// Change detected so lets add it to the list of changes
				if (change.getOperation() != EMPTY) {
					if (log.isDebugEnabled()) {
						log.debug("Field {" + fieldInB.getName() + "} was modified.");
					}
					changes.add(change);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Field {" + fieldInB.getName() + "} did not change.");
					}
				}
			}
		}

		// Compare the order of fields
		if (containerB.getFields().size() > 0) {
			compareAndAddOrderChange(changes, containerA, containerB, classOfFC);
		}

		// Compare the name
		compareAndAddSchemaProperty(changes, SchemaChangeModel.NAME_KEY, containerA.getName(), containerB.getName(), classOfFC);

		// Compare the description
		compareAndAddSchemaProperty(changes, SchemaChangeModel.DESCRIPTION_KEY, containerA.getDescription(), containerB.getDescription(), classOfFC, true);

		return changes;
	}

	/**
	 * Compare the schemas field order and determine whether the order of the listed fields was changed.
	 * 
	 * @param changes
	 * @param containerA
	 * @param containerB
	 * @param classOfFC
	 */
	private void compareAndAddOrderChange(List<SchemaChangeModel> changes, FC containerA, FC containerB, Class<? extends FC> classOfFC) {
		boolean hasChanges = false;

		List<String> fieldNames = new ArrayList<>();
		for (FieldSchema fieldSchema : containerB.getFields()) {
			fieldNames.add(fieldSchema.getName());
		}

		// The order has changed if the field size is different.
		if (containerB.getFields().size() != containerA.getFields().size()) {
			hasChanges = true;
		} else {
			// Field size is same. Lets compare the names per index
			for (int i = 0; i < containerA.getFields().size(); i++) {
				hasChanges = !fieldNames.get(i).equals(containerA.getFields().get(i).getName());
				if (hasChanges) {
					break;
				}
			}
		}

		if (hasChanges) {
			SchemaChangeModel change = createFieldContainerUpdateChange(classOfFC);
			change.getProperties().put(SchemaChangeModel.FIELD_ORDER_KEY, fieldNames.toArray());
			changes.add(change);
		}

	}

	/**
	 * Transform the fields of the field container into a map in which the key is the name of the field.
	 * 
	 * @param container
	 * @return
	 */
	private Map<String, FieldSchema> transformFieldsToMap(FC container) {
		Map<String, FieldSchema> map = new HashMap<>();
		for (FieldSchema field : container.getFields()) {
			map.put(field.getName(), field);
		}
		return map;

	}
	/**
	 * Compare the given objects and add a schema change entry to the given list of changes.
	 * 
	 * @param changes
	 * @param key
	 * @param objectA
	 * @param objectB
	 * @param classOfFC
	 */
	protected void compareAndAddSchemaProperty(List<SchemaChangeModel> changes, String key, Object objectA, Object objectB,
		Class<? extends FC> classOfFC) {
		compareAndAddSchemaProperty(changes, key, objectA, objectB, classOfFC, false);
	}

	/**
	 * Compare the given objects and add a schema change entry to the given list of changes.
	 * 
	 * @param changes
	 * @param key
	 * @param objectA
	 * @param objectB
	 * @param classOfFC
	 * @param nullIsEmpty true if null value is logically equal to an empty value
	 */
	protected void compareAndAddSchemaProperty(List<SchemaChangeModel> changes, String key, Object objectA, Object objectB,
		Class<? extends FC> classOfFC, boolean nullIsEmpty) {
		if (!CompareUtils.equals(objectA, objectB)) {
			if (nullIsEmpty && (
					(objectB == null && objectA != null && StringUtils.isEmpty(objectA.toString())) ||
					(objectA == null && objectB != null && StringUtils.isEmpty(objectB.toString()))
				)) {
				return;
			}
			SchemaChangeModel change = createFieldContainerUpdateChange(classOfFC);
			change.getProperties().put(key, objectB);
			changes.add(change);
		}
	}

	/**
	 * Compare the given Elasticsearch properties and add a schema change entry to the given list of changes.
	 *
	 * @param changes
	 * @param key
	 * @param objectA
	 * @param objectB
	 * @param classOfFC
	 */
	protected void compareAndAddSchemaElasticSearchProperty(List<SchemaChangeModel> changes, String key, JsonObject objectA, JsonObject objectB,
		Class<? extends FC> classOfFC) {
		// Empty objects and null/missing values should be treated the same
		if (objectA == null) {
			objectA = new JsonObject();
		}
		if (objectB == null) {
			objectB = new JsonObject();
		}
		compareAndAddSchemaProperty(changes, key, objectA, objectB, classOfFC);
	}

	/**
	 * Return the specific update change for the field container.
	 * 
	 * @param classOfFC
	 * @return
	 */
	private SchemaChangeModel createFieldContainerUpdateChange(Class<? extends FC> classOfFC) {
		if (SchemaModel.class.isAssignableFrom(classOfFC)) {
			return SchemaChangeModel.createUpdateSchemaChange();
		} else if (MicroschemaModel.class.isAssignableFrom(classOfFC)) {
			return SchemaChangeModel.createUpdateMicroschemaChange();
		} else {
			throw error(INTERNAL_SERVER_ERROR, "Unknown field container type {" + classOfFC.getName() + "}");
		}
	}

}
