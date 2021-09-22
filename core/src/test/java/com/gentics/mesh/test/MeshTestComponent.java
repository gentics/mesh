package com.gentics.mesh.test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.SearchProviderType;
import com.gentics.mesh.dagger.module.BindModule;
import com.gentics.mesh.dagger.module.DebugInfoProviderModule;
import com.gentics.mesh.dagger.module.MeshModule;
import com.gentics.mesh.dagger.module.MicrometerModule;
import com.gentics.mesh.dagger.module.PluginModule;
import com.gentics.mesh.dagger.module.SearchProviderModule;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.SearchWaitUtil;
import dagger.BindsInstance;
import dagger.Component;

import javax.annotation.Nullable;
import javax.inject.Singleton;

@Singleton
@Component(modules = {MeshModule.class, PluginModule.class, SearchProviderModule.class, BindModule.class, DebugInfoProviderModule.class, MicrometerModule.class})
public interface MeshTestComponent extends MeshComponent {

	@Component.Builder
	interface Builder {
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