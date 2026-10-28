package com.gentics.mesh.plugin.search.generator;

import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.generator.PluginOpenApiPreparer;
import com.gentics.mesh.plugin.common.generator.PluginOpenApiSpecGenerator;
import com.gentics.mesh.plugin.search.SearchPlugin;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;

/**
 * SearchPlugin#createProjectRouter() eagerly reads its own config field (for custom query handlers), normally
 * populated in initialize(), which also performs license/config validation. Pre-populate it with a plain default
 * configuration instead.
 */
public class SearchOpenApiPreparer implements PluginOpenApiPreparer {

	@Override
	public void prepare(RestPlugin plugin) throws ReflectiveOperationException {
		PluginOpenApiSpecGenerator.setField(SearchPlugin.class, plugin, "config", new SearchConfiguration());
	}
}
