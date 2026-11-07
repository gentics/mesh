package com.gentics.mesh.plugin.mustread.generator;

import java.lang.reflect.Method;

import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.generator.PluginOpenApiPreparer;
import com.gentics.mesh.plugin.common.generator.PluginOpenApiSpecGenerator;
import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;

/**
 * MustReadPlugin#createProjectRouter() eagerly calls its own caching getConfiguration(), which - unless already
 * populated - reads (and, if missing, writes) a config.yml file from disk. Pre-populate the field with the same
 * default the plugin itself would generate, so offline route registration never touches the filesystem.
 */
public class MustReadOpenApiPreparer implements PluginOpenApiPreparer {

	@Override
	public void prepare(RestPlugin plugin) throws ReflectiveOperationException {
		MustReadPlugin mustReadPlugin = (MustReadPlugin) plugin;
		Method generateDefault = MustReadPlugin.class.getDeclaredMethod("generateDefaultConfiguration");
		generateDefault.setAccessible(true);
		Object defaultConfig = generateDefault.invoke(plugin);
		PluginOpenApiSpecGenerator.setField(MustReadPlugin.class, plugin, "configuration", defaultConfig);
		// createProjectRouter() references the "logHelper" field directly rather than through its own lazy
		// getLogHelper() accessor, so it must be pre-populated the same way that accessor would.
		PluginOpenApiSpecGenerator.setField(MustReadPlugin.class, plugin, "logHelper", new LogHelper(mustReadPlugin));
	}
}
