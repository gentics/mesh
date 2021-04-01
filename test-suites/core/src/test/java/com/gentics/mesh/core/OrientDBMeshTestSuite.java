package com.gentics.mesh.core;

import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.context.MeshTestContextProvider;
import com.gentics.mesh.test.context.MeshTestContextSuite;

/**
 * OrientDB-contexted {@link MeshTestContextSuite} implementation.
 * 
 * @author plyhun
 *
 */
public class OrientDBMeshTestSuite extends MeshTestContextSuite {
	
    public OrientDBMeshTestSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
    }

	@Override
	protected Class<? extends MeshTestContextProvider> getTestContextProviderClass() {
		return OrientDBTestContextProvider.class;
	}

	@Override
	protected Class<? extends MeshOptionsProvider> getOptionsProviderClass() {
		return OrientDBMeshOptionsProvider.class;
	}
}
