package com.gentics.mesh.test.context;

import java.io.IOException;

import com.gentics.mesh.MeshOptionsTypeUnawareContext;

public interface MeshOptionsProvider extends MeshOptionsTypeUnawareContext {
	
	void initStorage(MeshTestSetting settings) throws IOException;
	
	void initFolders(ThrowingFunction<String, String, IOException> pathProvider) throws IOException;

	void cleanupPhysicalStorage() throws IOException;
}
