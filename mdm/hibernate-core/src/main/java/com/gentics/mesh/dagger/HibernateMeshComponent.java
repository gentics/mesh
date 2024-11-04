package com.gentics.mesh.dagger;

import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.cache.ListableFieldCacheImpl;
import com.gentics.mesh.contentoperation.ContentCachedStorage;
import com.gentics.mesh.dagger.module.AuthChainProviderModule;
import com.gentics.mesh.dagger.module.CommonModule;
import com.gentics.mesh.dagger.module.SearchWaitUtilProviderModule;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.SearchWaitUtil;

import dagger.BindsInstance;
import dagger.Component;

/**
 * Central dagger mesh component which will expose dependencies.
 */
@Singleton
@Component(modules = { CommonModule.class, HibernateModule.class, SearchWaitUtilProviderModule.class, AdminEndpointProviderModule.class, DatabaseConnectorModule.class, AuthChainProviderModule.class })
public interface HibernateMeshComponent extends MeshComponent {

	@Getter
	ContentCachedStorage contentCacheStorage();

	@Getter
	ListableFieldCacheImpl listableFieldCacheStorage();

	@Component.Builder
	interface Builder extends MeshComponent.Builder {
		@BindsInstance
		Builder configuration(MeshOptions options);

		@BindsInstance
		Builder mesh(Mesh mesh);

		@BindsInstance
		Builder searchProviderType(@Nullable SearchProviderType type);

		@BindsInstance
		Builder searchWaitUtilSupplier(@Nullable Supplier<SearchWaitUtil> swUtil);

		HibernateMeshComponent build();
	}
}