package com.gentics.mesh.dagger.module;

import dagger.Module;

@Module(includes = { MeshModule.class, PluginModule.class, SearchProviderModule.class, DebugInfoProviderModule.class, MicrometerModule.class, DaoTransformableModule.class, CommonBindModule.class })
public class CommonModule {
}
