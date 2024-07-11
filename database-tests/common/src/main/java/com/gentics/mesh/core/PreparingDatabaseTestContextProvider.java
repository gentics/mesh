package com.gentics.mesh.core;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.docker.PreparingDatabaseContainer;
import com.gentics.mesh.test.MeshProviderOrder;

/**
 * Abstract base class for context providers, which start a new container and will prepare the databases in advance
 *
 * @param <T> type of the database container
 */
@MeshProviderOrder(2)
public abstract class PreparingDatabaseTestContextProvider<T extends PreparingDatabaseContainer<T>> extends DatabaseTestContextProvider<T> {

	protected String current;

	/**
	 * Create an instance
	 * @param databaseContainer database container
	 */
	public PreparingDatabaseTestContextProvider(T databaseContainer) {
		super(databaseContainer);
	}

	@Override
	public void fillMeshOptions(HibernateMeshOptions options) {
		super.fillMeshOptions(options);
		current = getDatabaseContainer().take();
	}

	@Override
	public boolean fastStorageCleanup(Database db) throws Exception {
		getDatabaseContainer().dispose(current);
		current = getDatabaseContainer().take();
		fillMeshOptions(meshOptions);
		db.reset();
		return true;
	}
}
