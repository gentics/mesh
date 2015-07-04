package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.SchemaContainer;

public interface SchemaContainerRoot extends RootVertex<SchemaContainer> {

	SchemaContainer create(String name);

	void addSchemaContainer(SchemaContainer schemaContainer);

	void removeSchemaContainer(SchemaContainer schemaContainer);

	SchemaContainer findByName(String projectName, String name);

}
