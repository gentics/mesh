package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.MeshVertex;
import com.gentics.mesh.core.data.model.Schema;
import com.gentics.mesh.core.data.model.root.impl.SchemaRootImpl;

public interface SchemaRoot extends MeshVertex {

	Schema create(String name);

	void addSchema(Schema schema);

	List<? extends Schema> getSchemas();

	SchemaRootImpl getImpl();

}
