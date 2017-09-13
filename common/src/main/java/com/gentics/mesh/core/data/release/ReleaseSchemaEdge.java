package com.gentics.mesh.core.data.release;

import com.gentics.mesh.core.data.schema.SchemaContainerVersion;

public interface ReleaseSchemaEdge extends ReleaseVersionEdge {

	SchemaContainerVersion getSchemaContainerVersion();

}
