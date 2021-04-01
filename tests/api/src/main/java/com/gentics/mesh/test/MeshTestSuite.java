package com.gentics.mesh.test;

import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * An extension of JUnit4's test {@link Suite}, to automate running unit tests, that depend on 
 * an actual Mesh implementation, basing on {@link MeshOptions}.
 * 
 * @author plyhun
 *
 */
public abstract class MeshTestSuite extends Suite {
	
	/**
	 * Keep the constructor parameters list corresponding to the one calling in {@link Suite} via <code>@RunWith(Suite.class)</code>.
	 * See also {@link MeshOptionsProvider} for the implementation explanation.
	 * 
	 * @param klass
	 * @param builder
	 * @throws InitializationError
	 */
	public MeshTestSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
        System.setProperty("optionsProviderClass", getOptionsProviderClass().getCanonicalName());
    }
	
	/**
	 * Get the implementation of {@link MeshOptionsProvider} from the Mesh context.
	 * 
	 * @return
	 */
	protected abstract Class<? extends MeshOptionsProvider> getOptionsProviderClass();
}
