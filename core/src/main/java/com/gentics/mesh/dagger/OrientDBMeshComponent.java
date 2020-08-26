package com.gentics.mesh.dagger;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.module.BindModule;
import com.gentics.mesh.dagger.module.DaoModule;
import com.gentics.mesh.dagger.module.DebugInfoProviderModule;
import com.gentics.mesh.dagger.module.MeshModule;
import com.gentics.mesh.dagger.module.MicrometerModule;
import com.gentics.mesh.dagger.module.PluginModule;
import com.gentics.mesh.dagger.module.SearchProviderModule;
import com.gentics.mesh.etc.config.MeshOptions;

import dagger.BindsInstance;
import dagger.Component;

/**
 * Central dagger mesh component which will expose dependencies.
 */
@Singleton
@Component(modules = { MeshModule.class, PluginModule.class, SearchProviderModule.class, BindModule.class, DebugInfoProviderModule.class, MicrometerModule.class, DaoModule.class })
public interface OrientDBMeshComponent extends MeshComponent {
	@Component.Builder
	interface Builder extends MeshComponent.Builder {
		@BindsInstance
		Builder configuration(MeshOptions options);

		@BindsInstance
		Builder mesh(Mesh mesh);

		@BindsInstance
		Builder searchProviderType(@Nullable SearchProviderType type);

		OrientDBMeshComponent build();
	}
}
