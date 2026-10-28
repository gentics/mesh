package com.gentics.mesh.plugin.common.generator;

import com.gentics.mesh.plugin.RestPlugin;

/**
 * Reflectively pre-populates state a plugin's {@code createProjectRouter()}/{@code createGlobalRouter()}.
 */
public interface PluginOpenApiPreparer {

	void prepare(RestPlugin plugin) throws ReflectiveOperationException;
}
