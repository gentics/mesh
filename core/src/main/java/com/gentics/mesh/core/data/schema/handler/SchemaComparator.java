package com.gentics.mesh.core.data.schema.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModelImpl;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The schema comparator can be used to generate a set of {@link SchemaChange} objects by comparing two schemas. Some differences in between two schemas may
 * result in different changes. (eg. a field rename can also be mapped as an field removal + field addition)
 *
 */
@Component
public class SchemaComparator {

	private static final Logger log = LoggerFactory.getLogger(SchemaComparator.class);

	@Autowired
	private FieldSchemaComparator fieldComparator;

	public List<SchemaChangeModelImpl> diff(Schema schemaA, Schema schemaB) {
		Objects.requireNonNull(schemaA, "The schema must not be null");
		Objects.requireNonNull(schemaB, "The schema must not be null");

		List<SchemaChangeModelImpl> changes = new ArrayList<>();
		// segmentField
		compareAndAddSchemaProperty(changes, schemaA.getSegmentField(), schemaB.getSegmentField());

		// displayField
		compareAndAddSchemaProperty(changes, schemaA.getDisplayField(), schemaB.getDisplayField());

		// container flag
		compareAndAddSchemaProperty(changes, schemaA.isContainer(), schemaB.isContainer());

		// Diff the fields
		Map<String, FieldSchema> schemaAFields = transformFieldsToMap(schemaA);
		Map<String, FieldSchema> schemaBFields = transformFieldsToMap(schemaB);

		for (FieldSchema fieldInA : schemaA.getFields()) {
			// Check whether the field was removed in schemaB
			boolean wasRemoved = !schemaBFields.containsKey(fieldInA.getName());
			if (wasRemoved) {
				if (log.isDebugEnabled()) {
					log.debug("Field " + fieldInA.getName() + " was removed.");
				}
				changes.add(new SchemaChangeModelImpl().setOperation(SchemaChangeOperation.REMOVEFIELD));
			}
		}

		for (FieldSchema fieldInB : schemaB.getFields()) {
			FieldSchema fieldInA = schemaAFields.get(fieldInB.getName());

			// Check whether the field was added in schemaB 
			if (fieldInA == null) {
				if (log.isDebugEnabled()) {
					log.debug("Field " + fieldInB.getName() + " was added.");
				}
				changes.add(new SchemaChangeModelImpl().setOperation(SchemaChangeOperation.ADDFIELD));
			} else {
				// Field was not added or removed. It exists in both schemas. Lets see whether it changed
				Optional<SchemaChangeModelImpl> change = fieldComparator.compare(fieldInA, fieldInB);
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
					//TODO impl
				}
			}
		}

		return changes;
	}

	/**
	 * Compare the given objects and add a schema change entry to the given list of changes.
	 * 
	 * @param changes
	 * @param objectA
	 * @param objectB
	 */
	private void compareAndAddSchemaProperty(List<SchemaChangeModelImpl> changes, Object objectA, Object objectB) {
		switch (compare(objectA, objectB)) {

		case ADDED:
			changes.add(new SchemaChangeModelImpl().setOperation(SchemaChangeOperation.UPDATESCHEMA));
			break;
		case REMOVED:
			changes.add(new SchemaChangeModelImpl().setOperation(SchemaChangeOperation.UPDATESCHEMA));
			break;
		case CHANGED:
			changes.add(new SchemaChangeModelImpl().setOperation(SchemaChangeOperation.UPDATESCHEMA));
		}

	}

	/**
	 * Compare two strings and return a difference indicator.
	 * 
	 * @param a
	 * @param b
	 * @return Difference between both strings
	 */
	private Difference compare(Object a, Object b) {
		if (a == null && b == null) {
			return Difference.SAME;
		} else if (a == null && b != null) {
			return Difference.ADDED;
		} else if (b == null && a != null) {
			return Difference.REMOVED;
		} else if (a.equals(b)) {
			return Difference.SAME;
		} else {
			return Difference.CHANGED;
		}
	}

	/**
	 * Transform the fields of the schema into a map in which the key is the name of the field.
	 * 
	 * @param schema
	 * @return
	 */
	private Map<String, FieldSchema> transformFieldsToMap(Schema schema) {
		Map<String, FieldSchema> map = new HashMap<>();
		for (FieldSchema field : schema.getFields()) {
			map.put(field.getName(), field);
		}
		return map;

	}
}
