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
import com.gentics.mesh.core.data.schema.SchemaChangeAction;
import com.gentics.mesh.core.data.schema.impl.SchemaChangeImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;

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

	public List<SchemaChange> diff(Schema schemaA, Schema schemaB) {
		Objects.requireNonNull(schemaA, "The schema must not be null");
		Objects.requireNonNull(schemaB, "The schema must not be null");

		List<SchemaChange> changes = new ArrayList<>();
		// segmentField

		// displayField

		// container flag

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
			}
		}

		for (FieldSchema fieldInB : schemaB.getFields()) {
			FieldSchema fieldInA = schemaAFields.get(fieldInB.getName());

			// Check whether the field was added in schemaB 
			if (fieldInA == null) {
				if (log.isDebugEnabled()) {
					log.debug("Field " + fieldInB.getName() + " was added.");
				}
				// TODO impl 
				//changes.add(new SchemaChangeImpl().setAction(SchemaChangeAction.ADD));
			} else {
				// Field was not added or removed. It exists in both schemas. Lets see whether it changed
				Optional<SchemaChange> change = fieldComparator.compare(fieldInA, fieldInB);
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
