package com.gentics.mesh.core.schema;

import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_VERSION;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class SchemaDiffEndpointElasticSearchTest extends AbstractMeshTest {

	@Test
	public void testElasticSearchField() throws IOException {
		SchemaResponse schema = createESSchema();

		assertThat(getChanges(schema, schema.toJson())).isEmpty();
		assertThat(getChanges(schema, EsSchema.missingSchema())).isEmpty();
		assertThat(getChanges(schema, EsSchema.nullSchema())).isEmpty();
		assertThat(getChanges(schema, EsSchema.emptySchema())).isEmpty();
		assertThat(getChanges(schema, EsSchema.missingField())).isEmpty();
		assertThat(getChanges(schema, EsSchema.nullField())).isEmpty();
		assertThat(getChanges(schema, EsSchema.emptyField())).isEmpty();
		assertEquals(getChanges(schema, EsSchema.fieldMapping()).size(), 1);
		assertEquals(getChanges(schema, EsSchema.schemaMapping()).size(), 1);
	}

	private List<SchemaChangeModel> getChanges(SchemaResponse original, String updated) throws IOException {
		String body = httpClient().newCall(new Request.Builder()
			.addHeader("Authorization", "Bearer " + client().getAuthentication().getToken())
			.url(String.format("http://localhost:%d/api/v%d/schemas/%s/diff", port(), CURRENT_API_VERSION, original.getUuid()))
			.post(RequestBody.create(MediaType.parse("application/json"), updated))
			.build()
		).execute().body().string();
		return JsonUtil.readValue(body, SchemaChangesListModel.class).getChanges();
	}

	private SchemaResponse createESSchema() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setElasticsearch(new JsonObject());
		request.setName(EsSchema.NAME);
		request.setFields(Arrays.asList(
			new StringFieldSchemaImpl()
				.setName(EsSchema.FIELD_NAME)
				.setElasticsearch(new JsonObject())
		));
		return client().createSchema(request).toSingle().blockingGet();
	}

	private static class EsSchema {
		private static String NAME = "EsSchema";
		private static String FIELD_NAME = "name";
		private static ObjectMapper mapper = new ObjectMapper();

		private static ObjectNode basicRequest() {
			return (ObjectNode) mapper.createObjectNode()
				.put("name", NAME)
				.set("fields", mapper.createArrayNode()
					.add(mapper.createObjectNode()
						.put("name", FIELD_NAME)
						.put("type", "string")
					)
				);
		}

		private static ObjectNode field(ObjectNode request) {
			return (ObjectNode) request
				.get("fields")
				.get(0);
		}

		public static String missingField() {
			return basicRequest().toString();
		}

		public static String nullField() {
			ObjectNode request = basicRequest();
			field(request)
				.set("elasticsearch", NullNode.getInstance());
			return request.toString();
		}

		public static String missingSchema() {
			return basicRequest().toString();
		}

		public static String nullSchema() {
			return basicRequest()
				.set("elasticsearch", NullNode.getInstance())
				.toString();
		}

		public static String fieldMapping() {
			ObjectNode request = basicRequest();
			field(request)
				.set("elasticsearch", mapper.createObjectNode()
					.set("raw", mapper.createObjectNode()
						.put("type", "keyword")
					)
				);
			return request.toString();
		}

		public static String schemaMapping() {
			return basicRequest()
				.set("elasticsearch", mapper.createObjectNode()
					.set("analysis", mapper.createObjectNode()
						.set("stop", mapper.createObjectNode()
							.put("type", "stop")
							.put("stopwords", "_english_")
						)
					)
				).toString();
		}

		public static String emptySchema() {
			return basicRequest()
				.set("elasticsearch", mapper.createObjectNode())
				.toString();
		}

		public static String emptyField() {
			ObjectNode request = basicRequest();
			field(request)
				.set("elasticsearch", mapper.createObjectNode())
				.toString();
			return request.toString();
		}
	}

}
