package com.gentics.mesh.dagger.module;

import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.ActiveConfigProvider;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

@Module
public class DebugInfoProviderModule {

	@Provides @IntoSet
	public static DebugInfoProvider activeConfigProvider(ActiveConfigProvider provider) {
		return provider;
	}
}
