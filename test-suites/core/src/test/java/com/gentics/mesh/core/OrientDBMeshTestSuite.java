package com.gentics.mesh.core;

import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.gentics.mesh.dagger.DaggerOrientDBMeshComponent;
import com.gentics.mesh.dagger.MeshComponent.Builder;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.test.context.MeshOptionsProvider;
import com.gentics.mesh.test.context.MeshTestContextSuite;
import com.gentics.mesh.test.context.OrientDBMeshOptionsProvider;

public class OrientDBMeshTestSuite extends MeshTestContextSuite {
	
    public OrientDBMeshTestSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
    }

	@Override
	protected MeshOptions spawnOptions() {
		return new OrientDBMeshOptions();
	}

	@Override
	protected MeshOptionsProvider spawnOptionsProvider() {
		return new OrientDBMeshOptionsProvider();
	}

	@Override
	protected Builder spawnComponentBuilder() {
		return DaggerOrientDBMeshComponent.builder();
	}
}
