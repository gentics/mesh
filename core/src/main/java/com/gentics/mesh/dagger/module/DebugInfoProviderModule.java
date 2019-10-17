package com.gentics.mesh.dagger.module;

import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.ActiveConfigProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.BinaryDiskUsageProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.ConfigProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.ConsistencyCheckProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.DatabaseDumpProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.EntitiesProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.LogProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.MigrationStatusProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.PluginsProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.StatusProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.SystemInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.providers.ThreadDumpProvider;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;

@Module
public abstract class DebugInfoProviderModule {

	@Binds @IntoSet
	public abstract DebugInfoProvider activeConfigProvider(ActiveConfigProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider threadDumpProvider(ThreadDumpProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider systemInfoProvider(SystemInfoProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider entitiesProvider(EntitiesProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider pluginsProvider(PluginsProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider logProvider(LogProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider statusProvider(StatusProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider configProvider(ConfigProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider migrationStatusProvider(MigrationStatusProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider consistencyCheckProvider(ConsistencyCheckProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider binaryDiskUsageProvider(BinaryDiskUsageProvider provider);

	@Binds @IntoSet
	public abstract DebugInfoProvider databaseDumpProvider(DatabaseDumpProvider provider);
}
