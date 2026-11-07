package com.gentics.mesh.plugin.search;

import com.gentics.mesh.plugin.PluginConfigUtil;
import com.gentics.mesh.plugin.search.config.CustomQueryOptions;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if the configuration file can be loaded, and that fields with special behavior are interpreted correctly.
 */
public class ConfigTest {

	private SearchConfiguration config;

	@Before
	public void setup() throws IOException, URISyntaxException {
		config = PluginConfigUtil.loadConfig(
			new File(getClass().getResource("/config.yml").toURI()),
			null,
			SearchConfiguration.class);
	}

	/**
	 * Test if the shorthand notation for custom queries works as expected.
	 */
	@Test
	public void testCustomQueries() {
		assertThat(config.getCustomQueries())
			.as("Custom queries")
			.containsOnly(
					// This entry is configured using the shorthand notation.
					new CustomQueryOptions("custom_search", HandlerType.SEARCH, null, 0),
					// This entry has all three parts specified explicitly.
					new CustomQueryOptions("custom_with_result_query", HandlerType.SEARCH, "customResultQuery", 0),
					// This entry uses the default empty resultQuery.
					new CustomQueryOptions("custom_auto", HandlerType.AUTOCOMPLETE, null, 0),
					// This entry has the pageSize defined
					new CustomQueryOptions("custom_pagesize", null, null, 10)
			);
	}
}
