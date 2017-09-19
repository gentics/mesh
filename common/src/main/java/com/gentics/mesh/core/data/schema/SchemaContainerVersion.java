package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.Events.EVENT_SCHEMA_CREATED;
import static com.gentics.mesh.Events.EVENT_SCHEMA_DELETED;
import static com.gentics.mesh.Events.EVENT_SCHEMA_UPDATED;

import java.util.Iterator;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * Each schema update is stored within a dedicated schema container version in order to be able to keep track of changes in between different schema container
 * versions.
 */
public interface SchemaContainerVersion
		extends GraphFieldSchemaContainerVersion<SchemaResponse, SchemaModel, SchemaReference, SchemaContainerVersion, SchemaContainer> {

	static final String TYPE = "schemaVersion";

	static final TypeInfo TYPE_INFO = new TypeInfo(TYPE, EVENT_SCHEMA_CREATED, EVENT_SCHEMA_UPDATED, EVENT_SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return an iterator for {@link NodeGraphFieldContainer}'s that use this schema version and are DRAFT versions for the given release
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return
	 */
	Iterator<NodeGraphFieldContainer> getFieldContainers(String releaseUuid);

}
