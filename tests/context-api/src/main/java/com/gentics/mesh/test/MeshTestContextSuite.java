package com.gentics.mesh.test;

import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * An extension of JUnit4's test {@link Suite}, to automate running unit tests, that depend on 
 * an actual Mesh implementation, basing on {@link MeshOptions}, that use full {@link MeshTestContext}
 * 
 * @author plyhun
 *
 */
public abstract class MeshTestContextSuite extends MeshTestSuite {

	/**
	 * Keep the constructor parameters list corresponding to the one calling in {@link Suite} via <code>@RunWith(Suite.class)</code>.
	 * See also {@link MeshTestContextProvider} for the implementation explanation.
	 * 
	 * @param klass
	 * @param builder
	 * @throws InitializationError
	 */
	public MeshTestContextSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
		super(klass, builder);
		System.setProperty(MeshTestContextProvider.ENV_TEST_CONTEXT_PROVIDER_CLASS, getTestContextProviderClass().getCanonicalName());
	}
	
	/**
	 * Get the implementation of {@link MeshTestContextProvider} from the Mesh context.
	 * 
	 * @return
	 */
	protected abstract Class<? extends MeshTestContextProvider> getTestContextProviderClass();
}
