package com.gentics.mesh.test;

import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.gentics.mesh.etc.config.MeshOptions;

public abstract class MeshTestSuite extends Suite {
	
	private static MeshOptions options = null;

	public MeshTestSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
        MeshTestSuite.options = spawnOptions();
    }

	public static MeshOptions getOptions() {
		return options;
	}

	protected abstract MeshOptions spawnOptions();
}
