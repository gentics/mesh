package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATESCHEMA;

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
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractFieldSchemaContainerComparator<FC extends FieldSchemaContainer> {

	private static final Logger log = LoggerFactory.getLogger(AbstractFieldSchemaContainerComparator.class);

	@Autowired
	protected FieldSchemaComparator fieldComparator;

	public List<SchemaChangeModel> diff(FC containerA, FC containerB) throws IOException {
		Objects.requireNonNull(containerA, "containerA must not be null");
		Objects.requireNonNull(containerB, "containerB must not be null");

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
				SchemaChangeModel change = new SchemaChangeModel(REMOVEFIELD, fieldInA.getName());
				change.loadMigrationScript();
				changes.add(change);
			}
		}

		for (FieldSchema fieldInB : containerB.getFields()) {
			FieldSchema fieldInA = schemaAFields.get(fieldInB.getName());

			// Check whether the field was added in schemaB 
			if (fieldInA == null) {
				if (log.isDebugEnabled()) {
					log.debug("Field " + fieldInB.getName() + " was added.");
				}
				changes.add(new SchemaChangeModel(ADDFIELD, fieldInB.getName()));
			} else {
				// Field was not added or removed. It exists in both schemas. Lets see whether it changed
				Optional<SchemaChangeModel> change = fieldComparator.compare(fieldInA, fieldInB);
				// Change detected so lets add it to the list of changes
				if (change.isPresent()) {
					if (log.isDebugEnabled()) {
						log.debug("Field " + fieldInB.getName() + " was modified.");
					}
					changes.add(change.get());
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Field " + fieldInB.getName() + " did not change.");
					}
				}
			}
		}

		// order of fields
		compareAndAddOrderChange(changes, containerA, containerB);
		return changes;
	}

	/**
	 * Compare the schemas field order and determine whether the order of the listed fields was changed.
	 * 
	 * @param changes
	 * @param containerA
	 * @param containerB
	 */
	private void compareAndAddOrderChange(List<SchemaChangeModel> changes, FC containerA, FC containerB) {
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
			SchemaChangeModel change = new SchemaChangeModel();
			change.setOperation(UPDATESCHEMA);
			change.getProperties().put("order", fieldNames.toArray());
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
}
