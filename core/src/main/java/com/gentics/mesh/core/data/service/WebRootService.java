package com.gentics.mesh.core.data.service;

import com.gentics.mesh.path.Path;

public interface WebRootService {

	public Path findByProjectPath(String projectName, String path);

}
