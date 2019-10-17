package com.gentics.mesh.dagger.module;

import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.ActiveConfigProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.EntitiesProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.LogProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.PluginsProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.SystemInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.ThreadDumpProvider;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

@Module
public class DebugInfoProviderModule {

	@Provides @IntoSet
	public static DebugInfoProvider activeConfigProvider(ActiveConfigProvider provider) {
		return provider;
	}

	@Provides @IntoSet
	public static DebugInfoProvider threadDumpProvider(ThreadDumpProvider provider) {
		return provider;
	}

	@Provides @IntoSet
	public static DebugInfoProvider systemInfoProvider(SystemInfoProvider provider) {
		return provider;
	}

	@Provides @IntoSet
	public static DebugInfoProvider entitiesProvider(EntitiesProvider provider) {
		return provider;
	}

	@Provides @IntoSet
	public static DebugInfoProvider pluginsProvider(PluginsProvider provider) {
		return provider;
	}

	@Provides @IntoSet
	public static DebugInfoProvider logProvider(LogProvider provider) {
		return provider;
	}

	@Provides @IntoSet
	public static DebugInfoProvider statusProvider(StatusProvider provider) {
		return provider;
	}
}
