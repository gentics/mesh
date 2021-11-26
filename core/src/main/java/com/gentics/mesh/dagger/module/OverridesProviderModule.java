package com.gentics.mesh.dagger.module;

import java.util.function.Function;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.dagger.CoreMeshComponent;
import com.gentics.mesh.dagger.OverrideSupplier;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
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
	public static NodeIndexHandler provideNodeIndexHandler(@Nullable OverrideSupplier<NodeIndexHandler> provider, BootstrapInitializer boot) {
		return provideOrDefault(boot, provider, internal -> new NodeIndexHandlerImpl(internal.nodeContainerTransformer(), 
				internal.nodeContainerMappingProvider(), 
				internal.searchProvider(), 
				internal.database(), 
				internal.boot(), 
				internal.meshHelper(), 
				internal.options(), internal.syncMetersFactory(), 
				internal.bucketManager()));
	}

	private static <T> T provideOrDefault(BootstrapInitializer boot, @Nullable OverrideSupplier<T> provider, Function<CoreMeshComponent, T> defaultProvider) {
		if (provider != null) {
			return provider.get(boot.mesh());
		} else {
			return defaultProvider.apply(boot.mesh().internal());
		}
	}
}
