package com.gentics.mesh.cli;

import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootResolver;

/**
 * OrientDB-specific extension to {@link BootstrapInitializer}.
 * 
 * @author plyhun
 *
 */
public interface OrientDBBootstrapInitializer extends BootstrapInitializer {

	/**
	 * @deprecated This DAO getter will be moved onto MDM API once DAO itself is migrated. 
	 */
	@Getter
	@Deprecated
	ContentDaoWrapper contentDao();

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