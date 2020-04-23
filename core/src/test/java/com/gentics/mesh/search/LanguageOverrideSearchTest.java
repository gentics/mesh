package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = TestSize.PROJECT)
public class LanguageOverrideSearchTest extends AbstractMultiESTest {
	public LanguageOverrideSearchTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testLanguageOverride() {
		createSchema(loadResourceJsonAsPojo("schemas/languageOverride.json", SchemaCreateRequest.class));

		createPage("de", "Wetter", "Es ist herrliches Wetter, denn die Sonne kommt heraus.");
		createPage("en", "Report", "Many innocent people die during war.");

		createPage("de", "Kino",
			"Der Film \"Mirai – Das Mädchen aus der Zukunft\" (original \"Mirai no Mirai\") wurde zum besten Animationsfilm 2019 nominiert.");
		createPage("fr", "Film",
			"Le film \"Miraï, ma petite sœur\" (original \"Mirai no Mirai\") a été nominé pour le meilleur film d'animation en 2019.");
		createPage("it", "Cinema",
			"Il film \"Mirai\" (originale \"Mirai no Mirai\") è stato nominato come il miglior film d'animazione 2019.");
		createPage("en", "Corona", "As of April 2020, there is no vaccine for the Covid-19 virus.");
		createPage("zh", "Apology", "There is no chinese content yet.");
		createPage("ja", "Apology", "There is no japanese content yet.");
		createPage("ko", "Apology", "There is no korean content yet.");

		waitForSearchIdleEvent();

		// We expect only one result because "die" is a stop word in german
		assertContentSearch("die", "Report");
		// We expect only one result, because "no" is a stop word in all other languages.
		// It is not a stop word in french, but there is an exception in the schema for that.
		// There is no analyzer defined for italian, so the default english stop word list should be used.
		assertContentSearch("no", "Kino", "Cinema");
	}

	private NodeResponse createPage(String language, String title, String content) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest
			.setParentNode(getProject().getRootNode())
			.setLanguage(language)
			.setSchemaName("page")
			.setFields(FieldMap.of(
				"title", StringField.of(title),
				"content", StringField.of(content)
			));
		return client().createNode(PROJECT_NAME, nodeCreateRequest).blockingGet();
	}

	/**
	 * Executes a search query using the given <code>query</code>.
	 * Then asserts that the responses match the given <code>titles</code> in any order.
	 * @param query
	 * @param titles
	 */
	private void assertContentSearch(String query, String... titles) {
		NodeListResponse nodeListResponse = searchContent(query);
		assertThat(nodeListResponse.getData().stream()
			.map(node -> node.getFields().getStringField("title").getString()))
			.containsOnly(titles);
	}

	private NodeListResponse searchContent(String content) {
		return client().searchNodes(new JsonObject()
			.put("query", new JsonObject()
				.put("match", new JsonObject()
					.put("fields.content", content)
				)
			).toString()
		).blockingGet();
	}

}
