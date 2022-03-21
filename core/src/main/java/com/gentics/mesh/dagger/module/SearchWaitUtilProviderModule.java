package com.gentics.mesh.dagger.module;

import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.util.SearchWaitUtil;
import com.gentics.mesh.util.SearchWaitUtilImpl;

import dagger.Module;
import dagger.Provides;

@Module
public class SearchWaitUtilProviderModule {

	@Provides
	@Singleton
	public static SearchWaitUtil provideHandler(@Nullable Supplier<SearchWaitUtil> provider, MeshEventSender sender, MeshOptions options) {
		if (provider != null) {
			return provider.get();
		} else {
			return new SearchWaitUtilImpl(sender, options);
		}
	}
}
