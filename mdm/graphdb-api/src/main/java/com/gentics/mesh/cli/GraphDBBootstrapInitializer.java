package com.gentics.mesh.cli;

import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootResolver;

/**
 * GraphDB-specific extension to {@link BootstrapInitializer}.
 * 
 * @author plyhun
 *
 */
public interface GraphDBBootstrapInitializer extends BootstrapInitializer {
	/**
	 * Get the root vertex for the whole Mesh storage.
	 * 
	 * @return
	 */
	@Getter
	MeshRoot meshRoot();
	
	@Override
	default RootResolver rootResolver() {
		return meshRoot();
	}
}