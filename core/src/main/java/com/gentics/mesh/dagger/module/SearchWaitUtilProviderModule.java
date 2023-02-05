package com.gentics.mesh.dagger.module;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.SearchWaitUtil;
import com.gentics.mesh.util.SearchWaitUtilImpl;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class SearchWaitUtilProviderModule {
	@Provides
	@Singleton
	public static SearchWaitUtil provideHandler(MeshEventSender sender, MeshOptions options, MetricsService metrics) {
		return new SearchWaitUtilImpl(sender, options, metrics);
	}
}
