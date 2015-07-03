package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;

public interface SchemaContainerRoot extends RootVertex<SchemaContainer> {

	SchemaContainer create(String name);

	void addSchemaContainer(SchemaContainer schema);

	SchemaContainerRootImpl getImpl();

	SchemaContainer findByName(String projectName, String name);


}
