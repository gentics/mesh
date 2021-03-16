package com.gentics.mesh.test.context;

import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.test.MeshTestSuite;

public abstract class MeshTestContextSuite extends MeshTestSuite {

	private static MeshOptionsProvider optionsProvider = null;
	private static MeshComponent.Builder componentBuilder = null;

	public MeshTestContextSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
		super(klass, builder);
		MeshTestContextSuite.optionsProvider = spawnOptionsProvider();
		MeshTestContextSuite.componentBuilder = spawnComponentBuilder();
		init();
	}

	public static MeshOptionsProvider getOptionsProvider() {
		return optionsProvider;
	}

	public static MeshComponent.Builder getBuilderFactory() {
		return componentBuilder;
	}

	protected abstract MeshOptionsProvider spawnOptionsProvider();

	protected abstract MeshComponent.Builder spawnComponentBuilder();

	protected void init() {}
}
