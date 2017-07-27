package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * Each schema update is stored within a dedicated schema container version in order to be able to keep track of changes in between different schema container
 * versions.
 */
public interface SchemaContainerVersion extends GraphFieldSchemaContainerVersion<SchemaResponse, SchemaModel, SchemaReference, SchemaContainerVersion, SchemaContainer> {

	static String TYPE = "schemaVersion";

	/**
	 * Return a list {@link NodeGraphFieldContainer} that use this schema version and are DRAFT versions for the given release
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return
	 */
	List<? extends NodeGraphFieldContainer> getFieldContainers(String releaseUuid);

}
