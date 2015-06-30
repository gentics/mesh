package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;

public interface SchemaContainerRoot extends MeshVertex {

	SchemaContainer create(String name);

	void addSchemaContainer(SchemaContainer schema);

	List<? extends SchemaContainer> getSchemaContainers();

	SchemaContainerRootImpl getImpl();

}
