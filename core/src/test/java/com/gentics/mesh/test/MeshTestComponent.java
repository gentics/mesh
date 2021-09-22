package com.gentics.mesh.test;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.OrientDBMeshComponent;
import com.gentics.mesh.dagger.SearchProviderType;
import com.gentics.mesh.dagger.module.CommonModule;
import com.gentics.mesh.dagger.module.OrientDBModule;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.SearchWaitUtil;

import dagger.BindsInstance;
import dagger.Component;

@Singleton
@Component(modules = { CommonModule.class, OrientDBModule.class })
public interface MeshTestComponent extends OrientDBMeshComponent {

	@Component.Builder
	interface Builder extends OrientDBMeshComponent.Builder {
		@BindsInstance
		MeshTestComponent.Builder configuration(MeshOptions options);

		@BindsInstance
		MeshTestComponent.Builder mesh(Mesh mesh);

		@BindsInstance
		MeshTestComponent.Builder searchProviderType(@Nullable SearchProviderType type);

		@BindsInstance
		MeshTestComponent.Builder waitUtil(SearchWaitUtil util);

		MeshTestComponent build();
	}
}