package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.service.I18NUtil;
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

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class CustomIndexSettingsTest extends AbstractNodeSearchEndpointTest {

	/**
	 * Test the validation behaviour. Schema updates which include bogus json should fail early with a meaningful message.
	 */
	@Test
	public void testValidationErrorOnCreate() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.setElasticsearch(new JsonObject().put("somebogus", "value"));
		request.addField(FieldUtil.createStringFieldSchema("text").setElasticsearch(new JsonObject().put("bogus", "value")));
		call(() -> client().createSchema(request), BAD_REQUEST, "schema_error_index_validation",
				"Failed to parse mapping [default]: illegal field [bogus], only fields can be specified inside fields");
	}

	@Test
	public void testValidationErrorOnUpdate() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.setElasticsearch(new JsonObject().put("somebogus", "value"));
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
		request.setElasticsearch(new JsonObject().put("somebogus", "value"));
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
		request.setElasticsearch(new JsonObject().put("somebogus", "value"));
		request.addField(FieldUtil.createStringFieldSchema("text").setElasticsearch(IndexOptionHelper.getRawFieldOption()));
		SchemaResponse response = call(() -> client().createSchema(request));

		// Update settings and expect new version
		SchemaUpdateRequest updateRequest = JsonUtil.readValue(request.toJson(), SchemaUpdateRequest.class);
		updateRequest.setElasticsearch(new JsonObject().put("somebogus", "value2"));
		call(() -> client().updateSchema(response.getUuid(), updateRequest));
		SchemaResponse response2 = call(() -> client().findSchemaByUuid(response.getUuid()));
		assertEquals("value2", response2.getElasticsearch().getString("somebogus"));
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
		//assertNull(response4.getElasticsearch());
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
		schema.setElasticsearch(new JsonObject().put("somebogus", "value"));
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
		JsonObject elasticsearchSettings = getJson("/elasticsearch/suggestionSettings.json");
		schema.setElasticsearch(elasticsearchSettings);

		JsonObject fieldSettings = getJson("/elasticsearch/suggestionFieldMapping.json");
		schema.addField(FieldUtil.createStringFieldSchema("content").setElasticsearch(fieldSettings));
		System.out.println(schema.toJson());

		SchemaValidationResponse validationResponse = call(() -> client().validateSchema(schema));
		System.out.println(validationResponse.toJson());

		SchemaResponse response = call(() -> client().createSchema(schema));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, response.getUuid()));

		// 2. Create nodes
		Set<String> contents = new HashSet<>();
		String prefix = "This is<pre>another set of <strong>important</strong>content ";
		contents.add(prefix + "no text with more content you can poke a stick at");
		contents.add(prefix + "s<b>om</b>e text with more content you can poke a stick at");
		contents.add(prefix + "some <strong>more</strong> content you can poke a stick at too");
		contents.add(prefix + "someth<strong>ing</strong> completely different");
		contents.add(prefix + "some<strong>what</strong> strange content");

		for (String content : contents) {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.setSchemaName("customIndexTest");
			nodeCreateRequest.getFields().put("content", FieldUtil.createStringField(content));
			call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		}
		// 3. Invoke search
		// String didYouMeanQuery = getText("/elasticsearch/didYouMeanQuery.es");
		// JsonObject didYouMeanResult = call(() -> client().searchNodesRaw(PROJECT_NAME, didYouMeanQuery));
		// System.out.println(searchResult.encodePrettily());

		// String autocompleteQuery = getText("/elasticsearch/autocompleteQuery.es");
		// JsonObject autocompleteResult = call(() -> client().searchNodesRaw(PROJECT_NAME, autocompleteQuery));
		// System.out.println(autocompleteResult.encodePrettily());

		String autocompleteSuggest = getText("/elasticsearch/autocompleteSuggest.es");
		JsonObject autocompleteSuggestResult = call(() -> client().searchNodesRaw(PROJECT_NAME, autocompleteSuggest));
		System.out.println(autocompleteSuggestResult.encodePrettily());

	}

	@Test
	public void testSchemaValidationSuccess() {
		SchemaCreateRequest schema = new SchemaCreateRequest();
		schema.setName("settingsTest");
		schema.setElasticsearch(new JsonObject().put("somebogus", "value"));
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
