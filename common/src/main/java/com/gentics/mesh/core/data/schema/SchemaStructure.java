package com.gentics.mesh.core.data.schema;

import java.util.Optional;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface SchemaStructure extends MeshVertex {

	TraversalResult<SchemaContainerVersion> getSchemaContainerVersions();

	Optional<? extends SchemaContainerVersion> getLatestSchemaContainerVersion(String branchUuid);
}
