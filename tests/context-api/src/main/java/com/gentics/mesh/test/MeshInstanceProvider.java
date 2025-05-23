package com.gentics.mesh.test;

import java.util.List;

import org.testcontainers.utility.ThrowingFunction;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * A definition of the entity, that provides Mesh building blocks and functions to the test context.
 * 
 * @author plyhun
 *
 * @param <T> type of {@link MeshOptions} which belongs to the constructing Mesh instance
 */
public interface MeshInstanceProvider<T extends MeshOptions> extends MeshOptionsTypeAwareContext<T> {
	
	/**
	 * Initialize node storage for the test context.
	 * 
	 * @param settings attribute settings for the distinct test
	 * @throws Exception
	 */
	void initPhysicalStorage(MeshTestSetting settings) throws Exception;

	/**
	 * Prepare database for Mesh.
	 * 
	 * @param settings
	 * @param meshDagger
	 */
	void initMeshData(MeshTestSetting settings, MeshComponent meshDagger);

	/**
	 * Initialize filesystem for the test context.
	 * 
	 * @param pathProvider function that prepares the filesystem for the path, and gives the path back.
	 * @throws Exception
	 */
	void initFolders(ThrowingFunction<String, String> pathProvider) throws Exception;

	/**
	 * Reset the filesystem storage
	 * 
	 * @throws Exception
	 */
	void cleanupPhysicalStorage() throws Exception;
	
	/**
	 * Provides the component builder.
	 * 
	 * @return
	 */
	MeshComponent.Builder getComponentBuilder();

	/**
	 * Uninitialize node storage.
	 */
	void teardownStorage();

	/**
	 * Get the implementation of the test actions utility.
	 * @return
	 */
	MeshTestActions actions();

	/**
	 * Set DB specific write synchronization flag.
	 * 
	 * @param syncWrites
	 */
	void setSyncWrites(boolean syncWrites);

	/**
	 * Performs the fast storage cleanup, e.g. in place data drop with keeping the schemas, if available.
	 * 
	 * @param dbs list of databases to clean up
	 * @return true if the fast cleanup is available and succeeded.
	 * @throws Exception
	 */
	default boolean fastStorageCleanup(List<Database> dbs) throws Exception {
		return false;
	}
}
