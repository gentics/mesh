package com.gentics.mesh.plugin.search;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.extension.SearchQueryBuilder;
import com.gentics.mesh.plugin.search.service.query.BasicSearchQueryBuilder;
import com.gentics.mesh.plugin.search.service.query.TemplateSearchQueryBuilder;
import com.gentics.mesh.plugin.search.util.TestUtils;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SearchQueryBuilderTest {

	private enum QueryType {
		SEARCH("search.es"),
		AUTOCOMPLETE("autocomplete.es"),
		DID_YOU_MEAN_BINARY("did-you-mean.es"),
		DID_YOU_MEAN_CONTENT("did-you-mean.es"),
		CUSTOM("custom.es"),
		INVALID("invalid.es");

		public final String filename;

		QueryType(String filename) {
			this.filename = filename;
		}
	}

	private static final Pattern TEST_CASE_PATTERN = Pattern.compile("(\\S*)\\s*(\\S*)\\s*(\\S*)");
	private static final String PROJECT_NAME = "dummy";

	@Parameter
	public QueryType queryType;

	@Parameter(1)
	public String testCaseDefinition;

	private String queryStringKey;
	private SearchConfiguration config;
	private PluginContext pc;

	@Parameters(name = "{index}: query={0} params={1}")
	public static Collection<Object[]> data() throws URISyntaxException, FileNotFoundException {
		File queryStringsFile = new File(SearchQueryBuilder.class.getResource("/builder/queryStrings.txt").toURI());
		List<String> queryStrings = new BufferedReader(new FileReader(queryStringsFile)).lines()
			.map(String::trim)
			.filter(l -> l.charAt(0) != '#')
			.collect(Collectors.toList());
		Collection<Object[]> data = new ArrayList<>();

		for (QueryType type : QueryType.values()) {
			for (String queryString : queryStrings) {
				data.add(new Object[] { type, queryString });
			}
		}

		return data;
	}

	@Before
	public void setup() throws Exception {
		Matcher matcher = TEST_CASE_PATTERN.matcher(testCaseDefinition);

		if (!matcher.matches()) {
			throw new Exception("Invalid test case definition: " + testCaseDefinition);
		}

		Map<String, Set<String>> projects = new HashMap<>();

		projects.put(PROJECT_NAME, Stream.of(matcher.group(2).split(",")).collect(Collectors.toSet()));

		config = new SearchConfiguration().setProjects(projects);
		queryStringKey = matcher.group(1);
		pc = TestUtils.mockContext(PROJECT_NAME, "/search", matcher.group(3));
	}

	private String loadResource(String filename) throws IOException {
		URL resource = getClass().getResource(filename);

		System.out.println("Loading resource " + filename + ": " + resource);

		return IOUtils.toString(resource, StandardCharsets.UTF_8);
	}

	@Test
	public void testBasicSearchQueryBuilder() throws IOException {
		test(
			new BasicSearchQueryBuilder().init(config),
			loadResource("/builder/input/basic/" + queryType.filename));
	}

	@Test
	public void testTemplateSearchQueryBuilder() throws IOException {
		test(
			new TemplateSearchQueryBuilder().init(config),
			loadResource("/builder/input/template/" + queryType.filename));
	}

	private void test(SearchQueryBuilder builder, String input) throws IOException {
		JsonObject query;
		boolean failed;
		boolean shouldFail = queryType == QueryType.INVALID;
		JsonObject expected = queryType == QueryType.INVALID
			? null
			: new JsonObject(loadResource("/builder/expected/" + queryStringKey + "-" + queryType.filename));

		try {
			switch (queryType) {
			case SEARCH:
				query = builder.buildSearchQuery(pc, input);
				break;

			case AUTOCOMPLETE:
				query = builder.buildAutocompleteQuery(pc, input);
				break;

			case DID_YOU_MEAN_BINARY:
				query = builder.buildDidYouMeanQuery(pc, input, true);
				break;

			case DID_YOU_MEAN_CONTENT:
				query = builder.buildDidYouMeanQuery(pc, input, false);
				break;

			case CUSTOM:
				query = builder.buildQuery(pc, "custom-query", input);
				break;

			case INVALID:
				query = null;
				builder.buildSearchQuery(pc, input);
				builder.buildAutocompleteQuery(pc, input);
				builder.buildDidYouMeanQuery(pc, input, true);
				builder.buildDidYouMeanQuery(pc, input, false);
				break;

			default:
				query = new JsonObject().put("error", "Unhandled query type");
			}

			failed = false;
		} catch (Exception e) {
			System.out.println("Query creation failed: " + e.getMessage());
			query = null;
			failed = true;
		}

		assertThat(failed)
			.as("Building query should fail")
			.isEqualTo(shouldFail);

		assertThat(query)
			.as("Result query")
			.isEqualTo(expected);

	}
}
