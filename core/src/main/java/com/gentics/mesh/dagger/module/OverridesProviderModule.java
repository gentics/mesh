package com.gentics.mesh.dagger.module;

import java.util.function.Function;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.mesh.cache.ProjectBranchNameCache;
import com.gentics.mesh.cache.ProjectBranchNameCacheImpl;
import com.gentics.mesh.cache.ProjectNameCache;
import com.gentics.mesh.cache.ProjectNameCacheImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.OverrideSupplier;
import com.gentics.mesh.util.SearchWaitUtil;
import com.gentics.mesh.util.SearchWaitUtilImpl;

import dagger.Module;
import dagger.Provides;

@Module
public class OverridesProviderModule {

	@Provides
	@Singleton
	public static SearchWaitUtil provideSearchWaitUtil(@Nullable OverrideSupplier<SearchWaitUtil> provider, BootstrapInitializer boot) {
		return provideOrDefault(boot, provider, internal -> new SearchWaitUtilImpl(internal.meshEventSender(), internal.options()));
	}

	@Provides
	@Singleton
	public static ProjectNameCache provideProjectNameCache(@Nullable OverrideSupplier<ProjectNameCache> provider, BootstrapInitializer boot) {
		return provideOrDefault(boot, provider, internal -> new ProjectNameCacheImpl(internal.eventAwareCacheFactory(), internal.cacheRegistry()));
	}

	@Provides
	@Singleton
	public static ProjectBranchNameCache provideProjectBranchNameCache(@Nullable OverrideSupplier<ProjectBranchNameCache> provider, BootstrapInitializer boot) {
		return provideOrDefault(boot, provider, internal -> new ProjectBranchNameCacheImpl(internal.eventAwareCacheFactory(), internal.cacheRegistry()));
	}

	private static <T> T provideOrDefault(BootstrapInitializer boot, @Nullable OverrideSupplier<T> provider, Function<MeshComponent, T> defaultProvider) {
		if (provider != null) {
			return provider.get(boot.mesh());
		} else {
			return defaultProvider.apply(boot.mesh().internal());
		}
	}
}
