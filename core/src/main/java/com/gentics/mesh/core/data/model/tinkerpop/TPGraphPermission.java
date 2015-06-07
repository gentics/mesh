package com.gentics.mesh.core.data.model.tinkerpop;

import com.tinkerpop.frames.InVertex;
import com.tinkerpop.frames.OutVertex;

public interface TPGraphPermission extends TPAbstractPersistable {

	@InVertex
	public TPRole getRole();

	@OutVertex
	public TPAbstractPersistable getTargetNode();

	// TODO add permissions
}
