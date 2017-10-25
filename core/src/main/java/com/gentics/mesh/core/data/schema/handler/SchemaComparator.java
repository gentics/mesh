package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.CONTAINER_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DISPLAY_FIELD_NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.SEGMENT_FIELD_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.URLFIELDS_KEY;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * The schema comparator can be used to generate a set of {@link SchemaChange} objects by comparing two schemas. Some differences in between two schemas may
 * result in different changes. (eg. a field rename can also be mapped as an field removal + field addition)
 *
 */
@Singleton
public class SchemaComparator extends AbstractFieldSchemaContainerComparator<Schema> {

	@Inject
	public SchemaComparator() {
	}

	@Override
	public List<SchemaChangeModel> diff(Schema schemaA, Schema schemaB) {
		// diff fields
		List<SchemaChangeModel> changes = super.diff(schemaA, schemaB, Schema.class);

		// segmentField
		compareAndAddSchemaProperty(changes, SEGMENT_FIELD_KEY, schemaA.getSegmentField(), schemaB.getSegmentField(), Schema.class);

		// urlFields
		compareAndAddSchemaProperty(changes, URLFIELDS_KEY, schemaA.getUrlFields(), schemaB.getUrlFields(), Schema.class);

		// displayField
		compareAndAddSchemaProperty(changes, DISPLAY_FIELD_NAME_KEY, schemaA.getDisplayField(), schemaB.getDisplayField(), Schema.class);

		// container flag
		compareAndAddSchemaProperty(changes, CONTAINER_FLAG_KEY, schemaA.isContainer(), schemaB.isContainer(), Schema.class);

		return changes;
	}

}
