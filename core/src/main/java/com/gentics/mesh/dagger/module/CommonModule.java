package com.gentics.mesh.dagger.module;

import com.gentics.monitoring.liveness.dagger.LivenessManagerModule;
import dagger.Module;

/**
 * Main module which aggregates all sub modules.
 */
@Module(includes = {
	MeshModule.class,
	PluginModule.class,
	SearchProviderModule.class,
	DebugInfoProviderModule.class,
	MicrometerModule.class,
	DaoTransformableModule.class,
	CommonBindModule.class,
	JobProcessingModule.class,
	LivenessManagerModule.class,
	MeshLivenessModule.class
})
public class CommonModule {
}
