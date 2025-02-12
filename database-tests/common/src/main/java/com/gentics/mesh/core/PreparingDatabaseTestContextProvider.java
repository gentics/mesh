package com.gentics.mesh.core;

import java.util.List;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.dagger.MeshComponent;
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
		this(databaseContainer, null);
	}

	public PreparingDatabaseTestContextProvider(T databaseContainer, MeshComponent.Builder componentBuilder) {
		super(databaseContainer, componentBuilder);
	}

	@Override
	public void fillMeshOptions(HibernateMeshOptions options) {
		super.fillMeshOptions(options);
		current = getDatabaseContainer().take();
		options.getStorageOptions().setDatabaseAddress("localhost:" + getDatabaseContainer().getMappedPort());
	}

	@Override
	public boolean fastStorageCleanup(List<Database> dbs) throws Exception {
		getDatabaseContainer().dispose(current);
		fillMeshOptions(meshOptions);
		for (Database db : dbs) {
			db.reset();
		}
		return true;
	}
}
