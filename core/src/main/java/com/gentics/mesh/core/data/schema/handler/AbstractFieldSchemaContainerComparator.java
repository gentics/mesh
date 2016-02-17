package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractFieldSchemaContainerComparator<FC extends FieldSchemaContainer> {

	private static final Logger log = LoggerFactory.getLogger(AbstractFieldSchemaContainerComparator.class);

	@Autowired
	protected FieldSchemaComparator fieldComparator;

	/**
	 * Compare the two field containers. The implementor should invoke {@link #diff(FieldSchemaContainer, FieldSchemaContainer, Class)} and specifiy the actual
	 * field container class.
	 * 
	 * @param containerA
	 * @param containerB
	 * @return
	 * @throws IOException
	 */
	public abstract List<SchemaChangeModel> diff(FC containerA, FC containerB) throws IOException;

	protected List<SchemaChangeModel> diff(FC containerA, FC containerB, Class<? extends FC> classOfFC) throws IOException {
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
				change.loadMigrationScript();
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
				SchemaChangeModel change = SchemaChangeModel.createAddChange(fieldInB.getName(), fieldInB.getType());
				if (fieldInB instanceof ListFieldSchema) {
					change.setProperty("listType", ((ListFieldSchema) fieldInB).getListType());
				}

				if (i - 1 >= 0) {
					FieldSchema fieldBefore = containerB.getFields().get(i - 1);
					if (fieldBefore != null) {
						change.setProperty("after", fieldBefore.getName());
					}
				}
				changes.add(change);
			} else {
				// Field was not added or removed. It exists in both schemas. Lets see whether it changed
				Optional<SchemaChangeModel> change = fieldComparator.compare(fieldInA, fieldInB);
				// Change detected so lets add it to the list of changes
				if (change.isPresent()) {
					if (log.isDebugEnabled()) {
						log.debug("Field {" + fieldInB.getName() + "} was modified.");
					}
					changes.add(change.get());
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Field {" + fieldInB.getName() + "} did not change.");
					}
				}
			}
		}

		// order of fields
		compareAndAddOrderChange(changes, containerA, containerB, classOfFC);

		//name
		compareAndAddSchemaProperty(changes, SchemaChangeModel.NAME_KEY, containerA.getName(), containerB.getName(), classOfFC);

		// description
		compareAndAddSchemaProperty(changes, SchemaChangeModel.DESCRIPTION_KEY, containerA.getDescription(), containerB.getDescription(), classOfFC);

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

		// We don't need to add a change if the second container contains no or just one field.  
		if (containerB.getFields().size() <= 1) {
			return;
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
		if (!Objects.equals(objectA, objectB)) {
			SchemaChangeModel change = createFieldContainerUpdateChange(classOfFC);
			change.getProperties().put(key, objectB);
			changes.add(change);
		}
	}

	/**
	 * Return the specific update change for the field container.
	 * 
	 * @param classOfFC
	 * @return
	 */
	private SchemaChangeModel createFieldContainerUpdateChange(Class<? extends FC> classOfFC) {
		if (Schema.class.isAssignableFrom(classOfFC)) {
			return SchemaChangeModel.createUpdateSchemaChange();
		} else if (Microschema.class.isAssignableFrom(classOfFC)) {
			return SchemaChangeModel.createUpdateMicroschemaChange();
		} else {
			throw error(INTERNAL_SERVER_ERROR, "Unknown field container type {" + classOfFC.getName() + "}");
		}

	}

}
