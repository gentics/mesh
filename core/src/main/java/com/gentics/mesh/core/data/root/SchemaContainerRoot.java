package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaResponse;

public interface SchemaContainerRoot extends RootVertex<SchemaContainer, SchemaResponse> {

	SchemaContainer create(String name);

	void addSchemaContainer(SchemaContainer schemaContainer);

	void removeSchemaContainer(SchemaContainer schemaContainer);

	SchemaContainer findByName(String projectName, String name);

}
