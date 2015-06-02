package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import com.gentics.mesh.path.Path;

public interface WebRootService {

	public Path findByProjectPath(RoutingContext rc, String projectName, String path);

}
