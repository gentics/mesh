package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.AUTO_PURGE_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.CONTAINER_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DISPLAY_FIELD_NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ELASTICSEARCH_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.NO_INDEX_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.SEGMENT_FIELD_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.URLFIELDS_KEY;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * The schema comparator can be used to generate a set of {@link HibSchemaChange} objects by comparing two schemas. Some differences in between two schemas may
 * result in different changes. (eg. a field rename can also be mapped as an field removal + field addition)
 *
 */
@Singleton
public class SchemaComparatorImpl extends AbstractFieldSchemaContainerComparator<SchemaModel> implements SchemaComparator {

	@Inject
	public SchemaComparatorImpl() {
	}

	@Override
	public List<SchemaChangeModel> diff(SchemaModel schemaA, SchemaModel schemaB) {
		// .fields - diff fields
		List<SchemaChangeModel> changes = super.diff(schemaA, schemaB, SchemaModel.class);

		// .segmentField
		compareAndAddSchemaProperty(changes, SEGMENT_FIELD_KEY, schemaA.getSegmentField(), schemaB.getSegmentField(), SchemaModel.class);

		// .urlFields
		compareAndAddSchemaProperty(changes, URLFIELDS_KEY, schemaA.getUrlFields(), schemaB.getUrlFields(), SchemaModel.class);

		// .displayField
		compareAndAddSchemaProperty(changes, DISPLAY_FIELD_NAME_KEY, schemaA.getDisplayField(), schemaB.getDisplayField(), SchemaModel.class);

		// .autoPurge
		compareAndAddSchemaProperty(changes, AUTO_PURGE_FLAG_KEY, schemaA.getAutoPurge(), schemaB.getAutoPurge(), SchemaModel.class);

		// .noIndex
		compareAndAddSchemaProperty(changes, NO_INDEX_KEY, schemaA.getNoIndex(), schemaB.getNoIndex(), SchemaModel.class);

		// .container
		// Only diff the flag if a value has been set in the schemaB
		if (schemaB.getContainer() != null) {
			compareAndAddSchemaProperty(changes, CONTAINER_FLAG_KEY, schemaA.getContainer(), schemaB.getContainer(), SchemaModel.class);
		}

		// .elasticsearch
		compareAndAddSchemaElasticSearchProperty(changes, ELASTICSEARCH_KEY, schemaA.getElasticsearch(), schemaB.getElasticsearch(), SchemaModel.class);

		return changes;
	}

}
