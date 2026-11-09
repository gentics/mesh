package com.gentics.mesh.plugin.common.generator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.Pair;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.etc.config.Format;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.Version;
import com.gentics.mesh.plugin.MeshPluginDescriptor;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.util.MeshOpenAPIv3Generator;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * Builds a single commercial REST plugin's OpenAPI specification.
 */
public class PluginOpenApiSpecGenerator {

	private static final Logger log = LoggerFactory.getLogger(PluginOpenApiSpecGenerator.class);

	private final Vertx vertx;
	private final Map<Router, String> routers = new HashMap<>();

	public PluginOpenApiSpecGenerator(Vertx vertx) {
		this.vertx = vertx;
	}

	/**
	 * Construct a plugin instance with a mocked internals, if necessary.
	 *
	 * @param id the plugin id, matching its real deployment path segment
	 * @param factory the plugin's {@code (PluginWrapper, PluginEnvironment)} constructor
	 */
	public <T extends RestPlugin> T buildPlugin(String id, BiFunction<PluginWrapper, PluginEnvironment, T> factory) {
		PluginManager pluginManager = mock(PluginManager.class);
		when(pluginManager.getExtensions(any(Class.class))).thenReturn(Collections.emptyList());

		PluginManifest manifest = new PluginManifest().setId(id).setName(id);
		MeshPluginDescriptor descriptor = mock(MeshPluginDescriptor.class);
		when(descriptor.toPluginManifest()).thenReturn(manifest);

		PluginWrapper wrapper = mock(PluginWrapper.class);
		when(wrapper.getPluginId()).thenReturn(id);
		when(wrapper.getPluginManager()).thenReturn(pluginManager);
		when(wrapper.getDescriptor()).thenReturn(descriptor);

		MeshOptions options = mock(MeshOptions.class);
		when(options.getDefaultOpenAPIFormat()).thenReturn(Format.YAML);
		when(options.getDefaultOpenAPIVersion()).thenReturn(Version.V30);
		when(options.isServeOpenApi()).thenReturn(true);

		PluginEnvironment env = mock(PluginEnvironment.class);
		when(env.vertx()).thenReturn(vertx);
		when(env.options()).thenReturn(options);

		return factory.apply(wrapper, env);
	}

	/**
	 * Generate the OpenAPI v3 document and write it to {@code outputFile}.
	 */
	public void generate(String pluginName, String pluginId, RestPlugin plugin, File outputFile, String format, boolean useVersion31)
			throws IOException, com.gentics.vertx.openapi.model.OpenAPIGenerationException {
		Pair<Router, Router> routerPair = plugin.createRouters();
		Router globalRouter = routerPair.getLeft();
		Router projectRouter = routerPair.getRight();

		if (globalRouter != null) {
			routers.put(globalRouter, "/api/v" + MeshVersion.CURRENT_API_VERSION + "/plugins/" + pluginId);
		}
		if (projectRouter != null) {
			routers.put(projectRouter, "/api/v" + MeshVersion.CURRENT_API_VERSION + "/{project}/plugins/" + pluginId);
		}

		MeshOpenAPIv3Generator generator = new MeshOpenAPIv3Generator(MeshVersion.getPlainVersion(), Collections.emptyList(),
			true, Optional.empty(), Optional.empty());
		String yaml = generator.generate("Commercial Gentics Mesh " + pluginName + " REST API", routers,
			com.gentics.vertx.openapi.model.Format.parse(format), true, useVersion31);
		writeFile(outputFile, yaml);
	}

	private static void writeFile(File outputFile, String content) throws IOException {
		File parent = outputFile.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
		Files.write(outputFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
		log.info("File saved to {" + outputFile.getPath() + "}");
	}

	/**
	 * Reflectively set a (possibly private) field. For API spec generation usage only.
	 */
	public static void setField(Class<?> declaringClass, Object target, String fieldName, Object value)
			throws ReflectiveOperationException {
		Field field = declaringClass.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
