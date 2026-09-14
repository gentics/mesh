package com.gentics.mesh.plugin.common.generator;

import java.io.File;
import java.util.function.BiFunction;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.Vertx;

/**
 * Build-time entry point invoked by each commercial plugin's own exec-maven-plugin execution to generate that
 * plugin's standalone OpenAPI v3 spec, written to {@code target/classes/META-INF/openapi/openapi.yaml} so it
 * ends up bundled inside the plugin's own jar (its {@code process-classes} phase binding runs before the jar/
 * shade plugin packages {@code target/classes}).
 *
 * <p>
 * Arguments: {@code <pluginId> <pluginClass> <outputFile> [preparerClass]} - {@code pluginClass} must have a
 * {@code (PluginWrapper, PluginEnvironment)} constructor (as every {@code RestPlugin} does), and the optional
 * {@code preparerClass} must implement {@link PluginOpenApiPreparer} with a public no-arg constructor.
 */
public class PluginOpenApiGeneratorRunner {

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			throw new IllegalArgumentException(
				"Usage: " + PluginOpenApiGeneratorRunner.class.getName() + " <pluginName> <pluginId> <pluginClass> <outputFile> [preparerClass]");
		}
		String pluginName = args[0];
		String pluginId = args[1];
		String pluginClassName = args[2];
		File outputFile = new File(args[3]);
		String preparerClassName = args.length > 4 ? args[4] : null;

		Vertx vertx = Vertx.vertx();
		try {
			@SuppressWarnings("unchecked")
			Class<? extends RestPlugin> pluginClass = (Class<? extends RestPlugin>) Class.forName(pluginClassName);
			BiFunction<PluginWrapper, PluginEnvironment, RestPlugin> factory = (wrapper, env) -> {
				try {
					return pluginClass.getConstructor(PluginWrapper.class, PluginEnvironment.class).newInstance(wrapper, env);
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("Could not construct plugin " + pluginClassName, e);
				}
			};

			PluginOpenApiSpecGenerator generator = new PluginOpenApiSpecGenerator(vertx);
			RestPlugin plugin = generator.buildPlugin(pluginId, factory);

			if (preparerClassName != null) {
				PluginOpenApiPreparer preparer = (PluginOpenApiPreparer) Class.forName(preparerClassName)
					.getDeclaredConstructor().newInstance();
				preparer.prepare(plugin);
			}

			generator.generate(pluginName, pluginId, plugin, outputFile, "yaml", false);
		} finally {
			vertx.close().await();
		}
	}
}
