package com.gentics.mesh.dagger;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.util.SearchWaitUtil;
import com.gentics.mesh.util.SearchWaitUtilImpl;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class SearchWaitUtilProvider {
	@Provides
	@Singleton
	public static SearchWaitUtil provideHandler(MeshEventSender sender, MeshOptions options) {
		return new SearchWaitUtilImpl(sender, options);
	}
}
