package com.gentics.mesh.plugin.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.etc.config.Format;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.Version;
import com.gentics.mesh.plugin.MeshPluginDescriptor;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.plugin.common.generator.PluginOpenApiSpecGenerator;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.search.config.CustomQueryOptions;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;

import io.vertx.core.Vertx;

/**
 * Tests that the routes for configured custom queries can be registered (SUP-20295).
 */
public class CustomEndpointRegistrationTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private Vertx vertx;

	@Before
	public void setup() {
		vertx = Vertx.vertx();
	}

	@After
	public void tearDown() {
		vertx.close();
	}

	/**
	 * Create the plugin routers with custom queries of both types configured, and check that the routes for the
	 * custom queries are registered and documented.
	 */
	@Test
	public void testCustomQueryRoutes() throws Exception {
		Set<CustomQueryOptions> customQueries = new HashSet<>();
		customQueries.add(new CustomQueryOptions("kit_tag_search", HandlerType.SEARCH, "searchResults", 0));
		customQueries.add(new CustomQueryOptions("custom_auto", HandlerType.AUTOCOMPLETE, null, 0));

		SearchPlugin plugin = buildPlugin();
		PluginOpenApiSpecGenerator.setField(SearchPlugin.class, plugin, "config", new SearchConfiguration().setCustomQueries(customQueries));

		File spec = new File(tmp.getRoot(), "openapi.yaml");
		new PluginOpenApiSpecGenerator(vertx).generate("Search", "search", plugin, spec, "yaml", false);

		assertThat(new String(Files.readAllBytes(spec.toPath()), StandardCharsets.UTF_8))
			.as("Generated OpenAPI specification")
			.contains("/custom/kit_tag_search")
			.contains("/custom/custom_auto");
	}

	/**
	 * Same mocks as {@link PluginOpenApiSpecGenerator#buildPlugin}, which cannot be used here, because it is compiled
	 * against a different Mockito version than the one on this module's test classpath.
	 */
	private SearchPlugin buildPlugin() {
		PluginManager pluginManager = mock(PluginManager.class);
		when(pluginManager.getExtensions(any(Class.class))).thenReturn(Collections.emptyList());

		MeshPluginDescriptor descriptor = mock(MeshPluginDescriptor.class);
		when(descriptor.toPluginManifest()).thenReturn(new PluginManifest().setId("search").setName("search"));

		PluginWrapper wrapper = mock(PluginWrapper.class);
		when(wrapper.getPluginId()).thenReturn("search");
		when(wrapper.getPluginManager()).thenReturn(pluginManager);
		when(wrapper.getDescriptor()).thenReturn(descriptor);

		MeshOptions options = mock(MeshOptions.class);
		when(options.getDefaultOpenAPIFormat()).thenReturn(Format.YAML);
		when(options.getDefaultOpenAPIVersion()).thenReturn(Version.V30);
		when(options.isServeOpenApi()).thenReturn(true);

		PluginEnvironment env = mock(PluginEnvironment.class);
		when(env.vertx()).thenReturn(vertx);
		when(env.options()).thenReturn(options);

		return new SearchPlugin(wrapper, env);
	}
}
