package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.validation.SchemaValidationResponse;
import com.gentics.mesh.core.rest.validation.ValidationStatus;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
@MeshTestSetting(elasticsearch = CONTAINER, testSize = FULL, startServer = true)
public class CustomIndexSettingsTest extends AbstractNodeSearchEndpointTest {

	private static final Logger log = LoggerFactory.getLogger(CustomIndexSettingsTest.class);

	/**
	 * Test the validation behaviour. Schema updates which include bogus json should fail early with a meaningful message.
	 */
	@Test
	public void testValidationErrorOnCreate() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.addField(FieldUtil.createStringFieldSchema("text").setElasticsearch(new JsonObject().put("bogus", "value")));
		call(() -> client().createSchema(request), BAD_REQUEST, "schema_error_index_validation",
			"Failed to parse mapping [default]: illegal field [bogus], only fields can be specified inside fields");
	}

	@Test
	public void testValidationErrorOnUpdate() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.addField(FieldUtil.createStringFieldSchema("text").setElasticsearch(IndexOptionHelper.getRawFieldOption()));
		SchemaResponse response = call(() -> client().createSchema(request));

		SchemaUpdateRequest updateRequest = JsonUtil.readValue(request.toJson(), SchemaUpdateRequest.class);
		updateRequest.removeField("text");
		updateRequest.addField(FieldUtil.createStringFieldSchema("text").setElasticsearch(new JsonObject().put("bogus", "value")));
		call(() -> client().updateSchema(response.getUuid(), updateRequest), BAD_REQUEST, "schema_error_index_validation",
			"Failed to parse mapping [default]: illegal field [bogus], only fields can be specified inside fields");
	}

	@Test
	public void testSuccessfulValidation() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.addField(FieldUtil.createStringFieldSchema("text").setElasticsearch(IndexOptionHelper.getRawFieldOption()));
		call(() -> client().createSchema(request));
	}

	/**
	 * Verify that the schema gets updated if only the index settings have been altered.
	 */
	@Test
	public void testSchemaDiff() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.setUrlFields("text");
		request.addField(FieldUtil.createStringFieldSchema("text").setElasticsearch(IndexOptionHelper.getRawFieldOption()));
		SchemaResponse response = call(() -> client().createSchema(request));

		// Update settings and expect new version
		SchemaUpdateRequest updateRequest = JsonUtil.readValue(request.toJson(), SchemaUpdateRequest.class);
		updateRequest.setElasticsearch(new JsonObject().put("number_of_shards", 3));
		call(() -> client().updateSchema(response.getUuid(), updateRequest));
		SchemaResponse response2 = call(() -> client().findSchemaByUuid(response.getUuid()));

		assertEquals(3, response2.getElasticsearch().getInteger("number_of_shards").intValue());
		assertNotEquals("The schema should have been updated by the introduced change but it was not.", response.getVersion(), response2
			.getVersion());
		assertThat(response2.getUrlFields()).containsOnly("text");

		// Set the settings to empty and update again
		updateRequest.setElasticsearch(new JsonObject());
		call(() -> client().updateSchema(response.getUuid(), updateRequest));
		SchemaResponse response3 = call(() -> client().findSchemaByUuid(response.getUuid()));
		assertTrue("The options should be empty", new JsonObject().equals(response3.getElasticsearch()));
		assertNotEquals("The schema should have been updated by the introduced change but it was not.", response2.getVersion(), response3
			.getVersion());
		assertThat(response3.getUrlFields()).containsOnly("text");

		updateRequest.setElasticsearch(null);
		call(() -> client().updateSchema(response.getUuid(), updateRequest));
		SchemaResponse response4 = call(() -> client().findSchemaByUuid(response.getUuid()));
		// TODO setting fields to null is not supported at this point of time. #196
		// assertNull(response4.getElasticsearch());
		assertThat(response4.getUrlFields()).containsOnly("text");
	}

	/**
	 * Verify that the schema gets updated if only the index settings of a field have been altered.
	 */
	@Test
	public void testSchemaFieldDiff() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.setUrlFields("text");
		request.addField(FieldUtil.createStringFieldSchema("text").setElasticsearch(IndexOptionHelper.getRawFieldOption()));
		SchemaResponse response = call(() -> client().createSchema(request));

		// Update the schema again with no alteration
		SchemaUpdateRequest updateRequest = JsonUtil.readValue(request.toJson(), SchemaUpdateRequest.class);
		call(() -> client().updateSchema(response.getUuid(), updateRequest));
		SchemaResponse response2 = call(() -> client().findSchemaByUuid(response.getUuid()));
		assertEquals("No new version should have been created.", response.getVersion(), response2.getVersion());

		// Update the schema again and remove the raw field
		updateRequest.getField("text").setElasticsearch(new JsonObject());
		call(() -> client().updateSchema(response.getUuid(), updateRequest));
		SchemaResponse response3 = call(() -> client().findSchemaByUuid(response.getUuid()));
		assertNotEquals("The schema should have been updated by the introduced change but it was not.", response.getVersion(), response3
			.getVersion());
	}

	@Test
	public void testSchemaValidationError() {
		SchemaCreateRequest schema = new SchemaCreateRequest();
		schema.setName("settingsTest");
		schema.addField(FieldUtil.createStringFieldSchema("text").setElasticsearch(new JsonObject().put("bogus", "value")));

		SchemaValidationResponse response = call(() -> client().validateSchema(schema));

		assertNotNull(response.getElasticsearch());
		assertEquals(ValidationStatus.INVALID, response.getStatus());

		String message = I18NUtil.get(Locale.ENGLISH, "schema_error_index_validation",
			"Failed to parse mapping [default]: illegal field [bogus], only fields can be specified inside fields");
		assertEquals(message, response.getMessage().getMessage());
		assertEquals("schema_error_index_validation", response.getMessage().getInternalMessage());

	}

	@Test
	public void testCustomAnalyzerAndQuery() throws IOException {

		// 1. Create schema
		SchemaCreateRequest schema = new SchemaCreateRequest();
		schema.setName("customIndexTest");
		JsonObject elasticsearchSettings = getJson("/elasticsearch/custom/suggestionSettings.json");
		schema.setElasticsearch(elasticsearchSettings);

		JsonObject fieldSettings = getJson("/elasticsearch/custom/suggestionFieldMapping.json");
		schema.addField(FieldUtil.createStringFieldSchema("content").setElasticsearch(fieldSettings));

		SchemaValidationResponse validationResponse = call(() -> client().validateSchema(schema));
		assertEquals(ValidationStatus.VALID, validationResponse.getStatus());

		SchemaResponse response = call(() -> client().createSchema(schema));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, response.getUuid()));

		// 2. Create nodes
		Set<String> contents = new HashSet<>();
		String prefix = "One this is<pre>another set of <strong>important</strong>content ";
		contents.add(prefix + "no text with more content you can poke a stick at");
		contents.add(prefix + "s<b>om</b>e text with more content test you can poke content the a convert stick at");
		contents.add(prefix
			+ "some <strong>more</strong> content text you content this Content thAmbalaru can poke a content Telefon connection stick at too");
		contents.add(prefix + "someth<strong>ing</strong> context completely conTent save different");
		contents.add(prefix + "some<strong>what</strong> strange content");

		for (String content : contents) {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.setSchemaName("customIndexTest");
			nodeCreateRequest.getFields().put("content", FieldUtil.createStringField(content));
			call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		}

		waitForSearchIdleEvent();

		// 3. Invoke search
		String searchQuery = getText("/elasticsearch/custom/customSearchQuery.es");
		JsonObject searchResult = new JsonObject(call(() -> client().searchNodesRaw(PROJECT_NAME, searchQuery)).toString());
		System.out.println(searchResult.encodePrettily());

		String query = "Content t";
		JsonObject autocompleteQuery = new JsonObject(getText("/elasticsearch/custom/autocompleteQuery.es"));
		autocompleteQuery.getJsonObject("query").getJsonObject("match").getJsonObject("fields.content.auto").put("query", query);
		JsonObject autocompleteResult = new JsonObject(call(() -> client().searchNodesRaw(PROJECT_NAME, autocompleteQuery.encodePrettily())).toString());
		System.out.println(autocompleteResult.encodePrettily());
		System.out.println("------------------------------");
		System.out.println(new JsonObject(parseResult(autocompleteResult, query)).encodePrettily());

	}

	final static String REGEX = "%ha%(.*?)%he%";
	final static Pattern HL_PATTERN = Pattern.compile(REGEX);

	private Map<String, Object> parseResult(JsonObject result, String query) {
		List<String> partials = Arrays.asList(query.split(" "));
		Map<String, Object> map = new HashMap<>();
		JsonArray hits = result
			.getJsonArray("responses")
			.getJsonObject(0)
			.getJsonObject("hits")
			.getJsonArray("hits");

		for (int i = 0; i < hits.size(); i++) {
			JsonObject hit = hits.getJsonObject(i);
			JsonArray highlights = hit.getJsonObject("highlight").getJsonArray("fields.content.auto");
			Set<String> foundTokens = new HashSet<>();
			for (int e = 0; e < highlights.size(); e++) {
				String firstHighlight = highlights.getString(e);
				// Remove all HTML
				firstHighlight = firstHighlight.replaceAll("<[^>]+>", "");

				final Matcher matcher = HL_PATTERN.matcher(firstHighlight);
				while (matcher.find()) {
					String part = matcher.group(1);
					foundTokens.add(part);
				}
			}
			if (log.isDebugEnabled()) {
				log.debug("Found tokens: " + foundTokens);
			}
			constructOptions(map, foundTokens, partials);
		}
		return map;
	}

	/**
	 * Construct autocompletion options from the found tokens and the initial set of partials.
	 *
	 * @param map
	 * @param foundTokens
	 * @param partials
	 */
	private void constructOptions(Map<String, Object> map, Set<String> foundTokens, List<String> partials) {
		StringBuffer baseBuffer = new StringBuffer();
		// First buildup the base string which contains the completed tokens.
		String lastUnknownPartial = null;
		for (String partial : partials) {
			if (log.isDebugEnabled()) {
				log.debug("Checking found tokens against partial {" + partial + "}");
			}
			if (foundTokens.stream().map(e -> e.toLowerCase()).collect(Collectors.toSet()).contains(partial.toLowerCase())) {
				baseBuffer.append(partial);
				foundTokens.remove(partial);
			} else {
				lastUnknownPartial = partial;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("Using last partial {" + lastUnknownPartial + "}");
		}

		// Now iterate over the remaining tokens and construct the full option
		String baseString = baseBuffer.toString();
		for (String tokenOption : foundTokens) {
			// Only generate auto complete options if the token matches the partial
			if (tokenOption.toLowerCase().startsWith(lastUnknownPartial.toLowerCase())) {
				if (log.isDebugEnabled()) {
					log.debug("Found token {" + tokenOption + "} which starts with the given last partial {" + lastUnknownPartial + "}");
				}
				StringBuffer optionBuffer = new StringBuffer();
				optionBuffer.append(baseString);
				if (!baseString.isEmpty()) {
					optionBuffer.append(" ");
				}
				optionBuffer.append(tokenOption);
				map.put(optionBuffer.toString(), optionBuffer.toString());
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Rejecting token {" + tokenOption + "} since it does not match up with the last partial {" + lastUnknownPartial + "}");
				}
			}
		}
	}

	@Test
	public void testSchemaValidationSuccess() {
		SchemaCreateRequest schema = new SchemaCreateRequest();
		schema.setName("settingsTest");
		SchemaValidationResponse response = call(() -> client().validateSchema(schema));
		assertNotNull(response.getElasticsearch());
		assertEquals(ValidationStatus.VALID, response.getStatus());
	}

	@Test
	public void testMicroschemaValidationError() {
		MicroschemaCreateRequest microschema = new MicroschemaCreateRequest();
		call(() -> client().validateMicroschema(microschema), BAD_REQUEST, "schema_error_no_name");
	}

	@Test
	public void testMicroschemaValidationSucess() {
		MicroschemaCreateRequest microschema = new MicroschemaCreateRequest();
		microschema.setName("someName");
		call(() -> client().validateMicroschema(microschema));
	}
}
