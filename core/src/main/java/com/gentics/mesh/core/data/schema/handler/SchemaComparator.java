package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATESCHEMA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The schema comparator can be used to generate a set of {@link SchemaChange} objects by comparing two schemas. Some differences in between two schemas may
 * result in different changes. (eg. a field rename can also be mapped as an field removal + field addition)
 *
 */
@Component
public class SchemaComparator extends AbstractFieldSchemaContainerComparator<Schema> {

	private static final Logger log = LoggerFactory.getLogger(SchemaComparator.class);

	@Override
	public List<SchemaChangeModel> diff(Schema schemaA, Schema schemaB) {
		List<SchemaChangeModel> changes = super.diff(schemaA, schemaB);

		// segmentField
		compareAndAddSchemaProperty(changes, "segmentField", schemaA.getSegmentField(), schemaB.getSegmentField());

		// displayField
		compareAndAddSchemaProperty(changes, "displayField", schemaA.getDisplayField(), schemaB.getDisplayField());

		// container flag
		compareAndAddSchemaProperty(changes, "container", schemaA.isContainer(), schemaB.isContainer());

		return changes;
	}

	/**
	 * Compare the given objects and add a schema change entry to the given list of changes.
	 * 
	 * @param changes
	 * @param key
	 * @param objectA
	 * @param objectB
	 */
	private void compareAndAddSchemaProperty(List<SchemaChangeModel> changes, String key, Object objectA, Object objectB) {
		if (!Objects.equals(objectA, objectB)) {
			SchemaChangeModel change = new SchemaChangeModel(UPDATESCHEMA);
			change.getProperties().put(key, objectB);
			changes.add(change);
		}
	}

}
