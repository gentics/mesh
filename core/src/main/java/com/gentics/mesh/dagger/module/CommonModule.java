package com.gentics.mesh.dagger.module;

import dagger.Module;

/**
 * Main module which aggregates all sub modules.
 */
@Module(includes = { MeshModule.class, PluginModule.class, SearchProviderModule.class, DebugInfoProviderModule.class, MicrometerModule.class,
	DaoTransformableModule.class, CommonBindModule.class, JobProcessingModule.class })
public class CommonModule {
}
