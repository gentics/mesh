package com.gentics.mesh.test.context;

import java.io.IOException;

import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionsTypeAwareContext;

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
	 * @throws IOException
	 */
	void initStorage(MeshTestSetting settings) throws IOException;
	
	/**
	 * Initialize filesystem for the test context.
	 * 
	 * @param pathProvider function that prepares the filesystem for the path, and gives the path back.
	 * @throws IOException
	 */
	void initFolders(ThrowingFunction<String, String, IOException> pathProvider) throws IOException;

	/**
	 * Reset the filesystem storage
	 * 
	 * @throws IOException
	 */
	void cleanupPhysicalStorage() throws IOException;
	
	/**
	 * Provides the component builder.
	 * 
	 * @return
	 */
	MeshComponent.Builder getComponentBuilder();	
	
}
