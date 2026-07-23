package com.gentics.mesh.dagger.module;

import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.monitoring.liveness.LivenessCheck;
import com.gentics.monitoring.liveness.LivenessManagerOptions;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

/**
 * Mesh-specific liveness configuration including plugin checks.
 */
@Module
public class MeshLivenessModule {

	@Provides
	static LivenessManagerOptions provideLivenessManagerOptions(MeshOptions options) {
		return new LivenessManagerOptions(options.getLivePath())
			.setMemoryLimit(options.getMonitoringOptions().getMemoryLimit())
			.setGcTimeLimit(options.getMonitoringOptions().getGcTimeLimit());
	}

	@Provides
	@IntoSet
	static LivenessCheck providePluginLivenessCheck(Lazy<MeshPluginManager> pluginManager) {
		return () -> {
			for (var id: pluginManager.get().getPluginIds()) {
				var status = pluginManager.get().getStatus(id);

				if (status == PluginStatus.FAILED) {
					return false;
				}
			}

			return true;
		};
	}
}
