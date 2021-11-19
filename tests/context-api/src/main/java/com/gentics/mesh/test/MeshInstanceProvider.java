package com.gentics.mesh.test;

import org.testcontainers.utility.ThrowingFunction;

import com.gentics.mesh.Mesh;
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
	 * @param mesh Mesh test instance
	 * @throws Exception
	 */
	void initStorage(MeshTestSetting settings, Mesh mesh) throws Exception;
	
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
}
