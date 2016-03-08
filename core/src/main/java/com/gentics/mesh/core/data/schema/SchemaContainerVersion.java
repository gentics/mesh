package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public interface SchemaContainerVersion extends GraphFieldSchemaContainerVersion<Schema, SchemaReference, SchemaContainerVersion, SchemaContainer> {

	/**
	 * Return a list {@link NodeGraphFieldContainer} that use this schema version.
	 * 
	 * @return
	 */
	List<? extends NodeGraphFieldContainer> getFieldContainers();

}
