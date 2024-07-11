package com.gentics.mesh.search;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaListModel;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringFieldModel;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = TestSize.PROJECT)
public class LanguageOverrideSearchTest extends AbstractMultiESTest {
	public LanguageOverrideSearchTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	private List<NodeResponse> createdPages;

	@Before
	public void setUp() throws Exception {
		createdPages = new ArrayList<>();
	}

	@Test
	public void testIndexCountAfterRemovingSettings() {
		int originalIndexCount = getIndexCount();
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		waitForSearchIdleEvent();
		// We expect 12 additional indices. (5 overridden languages + 1 default index) * 2 versions (draft, published)
		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 12);

		waitForJob(() -> {
			client().updateSchema(
				schema.getUuid(),
				loadResourceJsonAsPojo("schemas/languageOverride/pageNoEs.json", SchemaUpdateRequest.class)
			).blockingAwait();
		});
		waitForSearchIdleEvent();

		// All overrides have been removed. This leaves only the 2 default versions
		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 2);
	}

	@Test
	public void testSchemaMigration() {
		grantAdmin();
		int originalIndexCount = getIndexCount();
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		createContent();
		publishCreatedPages();
		waitForSearchIdleEvent();

		assertPublishedContentCount(schema.getUuid());

		// We expect 12 additional indices. (5 overridden languages + 1 default index) * 2 versions (draft, published)
		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 12);

		migrateSchema(schema.getName()).blockingAwait();
		waitForSearchIdleEvent();

		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 12);

		assertPublishedContentCount(schema.getUuid());
	}

	@Test
	public void testIndexCountAfterMigratingOldSchema() {
		grantAdmin();
		int originalIndexCount = getIndexCount();
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/pageNoEs.json", SchemaCreateRequest.class));
		waitForSearchIdleEvent();
		// The old schema has no overrides. This leaves only the 2 default versions
		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 2);

		waitForJob(() -> {
			client().updateSchema(
				schema.getUuid(),
				loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaUpdateRequest.class)
			).blockingAwait();
		});
		waitForSearchIdleEvent();


		// We expect 12 additional indices. (5 overridden languages + 1 default index) * 2 versions (draft, published)
		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 12);
	}

	@Test
	public void testIndexCountAfterDeletingSchema() {
		int originalIndexCount = getIndexCount();
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		waitForSearchIdleEvent();
		// We expect 12 additional indices. (5 overridden languages + 1 default index) * 2 versions (draft, published)
		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 12);

		deleteSchema(schema.getUuid());
		waitForSearchIdleEvent();
		assertThat(getIndexCount()).isEqualTo(originalIndexCount);
	}

	@Test
	public void testIndexCountAfterProjectDeletion() {
		int originalIndexCount = getIndexCount();
		ProjectResponse project = createProject();
		waitForSearchIdleEvent();
		// Tag Index + Folder Nodes (Draft, Published)
		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 3);
		createSchema(project.getName(), loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		waitForSearchIdleEvent();
		// We expect 12 additional indices. (5 overridden languages + 1 default index) * 2 versions (draft, published)
		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 15);

		deleteProject(project.getUuid());
		waitForSearchIdleEvent();
		assertThat(getIndexCount()).isEqualTo(originalIndexCount);
	}

	@Test
	public void testIndexCountAfterSync() throws Exception {
		int originalIndexCount = getIndexCount();
		createSchema(loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		waitForSearchIdleEvent();
		// We expect 12 additional indices. (5 overridden languages + 1 default index) * 2 versions (draft, published)
		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 12);

		recreateIndices();

		assertThat(getIndexCount()).isEqualTo(originalIndexCount + 12);
	}

	@Test
	public void testLanguageOverride() {
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		createContent();
		waitForSearchIdleEvent();

		assertContentCount(schema.getUuid());

		// We expect only one result because "die" is a stop word in german
		assertContentSearch("die", "Report");
		// We expect only two results, because "no" is a stop word in all languages except german and french.
		// French would use the standard analyzer, but there is an exception for that language in the schema.
		// There is no analyzer defined for italian, so the default english stop word list should be used.
		assertContentSearch("no", "Kino", "Film");

		publishCreatedPages();
		waitForSearchIdleEvent();

		assertPublishedContentCount(schema.getUuid());
	}

	@Test
	public void testIndexSyncWithMicronodeField() throws Exception {
		MicroschemaResponse microschema = createMicroschema("micro");
		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/pageWithMicronode.json", SchemaCreateRequest.class));
		createContent();
		waitForSearchIdleEvent();

		String microschemaHash = tx(tx -> {
			Schema schema1 = tx.schemaDao().findByUuid(schema.getUuid());
			SchemaVersion version = schema1.getLatestVersion();
			return version.getMicroschemaVersionHash(initialBranch());
		});

		assertDocumentCount(schema.getUuid(), fromEntries(
			docs("de", DRAFT, 2),
			docs("fr", DRAFT, 1),
			docs("zh", DRAFT, 1),
			docs("ja", DRAFT, 1),
			docs("ko", DRAFT, 1),
			// italian and english contents use default settings
			docs(microschemaHash.toLowerCase(), DRAFT, 3)
		));

		SyncEventHandler.invokeSyncCompletable(meshApi()).blockingAwait(30, TimeUnit.SECONDS);

		assertDocumentCount(schema.getUuid(), fromEntries(
			docs("de", DRAFT, 2),
			docs("fr", DRAFT, 1),
			docs("zh", DRAFT, 1),
			docs("ja", DRAFT, 1),
			docs("ko", DRAFT, 1),
			// italian and english contents use default settings
			docs(microschemaHash.toLowerCase(), DRAFT, 3)
		));

		// We expect only one result because "die" is a stop word in german
		assertContentSearch("die", "Report");
		// We expect only two results, because "no" is a stop word in all languages except german and french.
		// French would use the standard analyzer, but there is an exception for that language in the schema.
		// There is no analyzer defined for italian, so the default english stop word list should be used.
		assertContentSearch("no", "Kino", "Film");
	}

	@Test
	public void testIndexSync() throws Exception {
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		createContent();
		waitForSearchIdleEvent();

		assertContentCount(schema.getUuid());

		recreateIndices();

		assertContentCount(schema.getUuid());

		// We expect only one result because "die" is a stop word in german
		assertContentSearch("die", "Report");
		// We expect only two results, because "no" is a stop word in all languages except german and french.
		// French would use the standard analyzer, but there is an exception for that language in the schema.
		// There is no analyzer defined for italian, so the default english stop word list should be used.
		assertContentSearch("no", "Kino", "Film");
	}

	/**
	 * Check that documents are indexed in correct language specific indices after moving a node
	 * @throws Exception
	 */
	@Test
	public void testIndexAfterMove() throws Exception {
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		createContent();
		waitForSearchIdleEvent();

		assertContentCount(schema.getUuid());

		// create folder
		NodeResponse folder = createFolder("de", "Zielordner");

		// move first page into the target folder
		NodeResponse page = createdPages.get(0);
		client().moveNode(PROJECT_NAME, page.getUuid(), folder.getUuid()).blockingAwait();

		waitForSearchIdleEvent();

		assertContentCount(schema.getUuid());
	}

	/**
	 * Check that documents are indexed in correct generic indices after moving a node
	 * @throws Exception
	 */
	@Test
	public void testIndexAfterMoveGeneric() throws Exception {
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		createContent();
		waitForSearchIdleEvent();

		assertContentCount(schema.getUuid());

		// create folder
		NodeResponse folder = createFolder("de", "Zielordner");

		// move english page
		NodeResponse page = createdPages.get(1);
		client().moveNode(PROJECT_NAME, page.getUuid(), folder.getUuid()).blockingAwait();

		waitForSearchIdleEvent();

		assertContentCount(schema.getUuid());
	}

	/**
	 * Check that documents are indexed in correct language specific indices after tagging a node
	 * @throws Exception
	 */
	@Test
	public void testIndexAfterTagging() throws Exception {
		SchemaResponse schema = createSchema(loadResourceJsonAsPojo("schemas/languageOverride/page.json", SchemaCreateRequest.class));
		createContent();
		waitForSearchIdleEvent();

		assertContentCount(schema.getUuid());

		// add tag to first page
		NodeResponse page = createdPages.get(0);
		TagFamilyResponse family = client().createTagFamily(PROJECT_NAME, new TagFamilyCreateRequest().setName("colors")).blockingGet();
		TagResponse tag = client().createTag(PROJECT_NAME, family.getUuid(), new TagCreateRequest().setName("red")).blockingGet();
		client().addTagToNode(PROJECT_NAME, page.getUuid(), tag.getUuid()).blockingAwait();

		waitForSearchIdleEvent();

		assertContentCount(schema.getUuid());
	}

	private void createContent() {
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
	}

	private void assertContentCount(String schemaUuid) {
		assertDocumentCount(schemaUuid, fromEntries(
			docs("de", DRAFT, 2),
			docs("fr", DRAFT, 1),
			docs("zh", DRAFT, 1),
			docs("ja", DRAFT, 1),
			docs("ko", DRAFT, 1),
			// italian and english contents use default settings
			docs("en", DRAFT, 0),
			docs("it", DRAFT, 0),
			docs(DRAFT, 3)
		));
	}

	private void assertPublishedContentCount(String schemaUuid) {
		assertDocumentCount(schemaUuid, fromEntries(
			docs("de", DRAFT, 2),
			docs("fr", DRAFT, 1),
			docs("zh", DRAFT, 1),
			docs("ja", DRAFT, 1),
			docs("ko", DRAFT, 1),
			// italian and english contents use default settings
			docs("en", DRAFT, 0),
			docs("it", DRAFT, 0),
			docs(DRAFT, 3),

			docs("de", PUBLISHED, 2),
			docs("fr", PUBLISHED, 1),
			docs("zh", PUBLISHED, 1),
			docs("ja", PUBLISHED, 1),
			docs("ko", PUBLISHED, 1),
			// italian and english contents use default settings
			docs("en", PUBLISHED, 0),
			docs("it", PUBLISHED, 0),
			docs(PUBLISHED, 3)
		));

	}

	private NodeResponse createPage(String language, String title, String content) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest
			.setParentNode(getProject().getRootNode())
			.setLanguage(language)
			.setSchemaName("page")
			.setFields(FieldMap.of(
				"title", StringFieldModel.of(title),
				"content", StringFieldModel.of(content)
			));
		NodeResponse nodeResponse = client().createNode(PROJECT_NAME, nodeCreateRequest).blockingGet();
		createdPages.add(nodeResponse);
		return nodeResponse;
	}

	private void publishCreatedPages() {
		for (NodeResponse createdPage : createdPages) {
			publishNode(createdPage);
		}
	}

	/**
	 * Create a folder in the projects root node
	 * @param language folder language
	 * @param name folder name
	 * @return folder
	 */
	private NodeResponse createFolder(String language, String name) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest
			.setParentNode(getProject().getRootNode())
			.setLanguage(language)
			.setSchemaName("folder")
			.setFields(FieldMap.of(
				"name", StringFieldModel.of(name)
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
					.put("fields.content.basicsearch", content)
				)
			).toString(),
			new NodeParametersImpl().setLanguages("en", "de", "fr", "it", "zh", "ja", "ko")
		).blockingGet();
	}

	private int getIndexCount() {
		try {
			String response = httpClient().newCall(new Request.Builder()
				.url(getTestContext().getOptions().getSearchOptions().getUrl() + "/_all")
				.build()
			).execute().body().string();

			return new JsonObject(response).size();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private <K, V> Map<K, V> fromEntries(Map.Entry<K, V>... entries) {
		return Stream.of(entries)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private void assertDocumentCount(String schemaUuid,Map<Document, Long> expected) {
		try {
			BranchListResponse branches = client().findBranches(PROJECT_NAME).blockingGet();
			BranchResponse branch = branches.getData().stream().filter(BranchResponse::getLatest).findFirst()
					.orElseThrow(() -> new RuntimeException("Did not find latest branch for test project"));
			BranchInfoSchemaListModel schemaVersions = client().getBranchSchemaVersions(PROJECT_NAME, branch.getUuid()).blockingGet();
			BranchSchemaInfo info = schemaVersions.getSchemas().stream().filter(i -> StringUtils.equals(i.getUuid(), schemaUuid))
					.findFirst()
					.orElseThrow(() -> new RuntimeException("Did not find version of schema " + schemaUuid));

			String response = httpClient().newCall(new Request.Builder()
				.url(getTestContext().getOptions().getSearchOptions().getUrl() + "/mesh-node-*-" + info.getVersionUuid() + "-*/_search")
				.method("POST", RequestBody.create(MediaType.parse("application/json"), new JsonObject()
					.put("size", 100)
					.put("query", new JsonObject()
						.put("query_string", new JsonObject()
								.put("query", "*")
						)
					).toString()
				))
				.build()
			).execute().body().string();

			Map<Document, Long> actual = new JsonObject(response)
				.getJsonObject("hits").getJsonArray("hits")
				.stream()
				.map(doc -> (JsonObject) doc)
				.map(Document::fromElasticsearch)
				.collect(Collectors.groupingBy(
					Function.identity(),
					Collectors.counting()
				));

			for (Document key : expected.keySet()) {
				actual.computeIfAbsent(key, k -> 0L);
			}

			assertThat(actual).isEqualTo(expected);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Map.Entry<Document, Long> docs(ContainerType type, long count) {
		return docs(null, type, count);
	}

	private Map.Entry<Document, Long> docs(String language, ContainerType type, long count) {
		return new AbstractMap.SimpleImmutableEntry<>(new Document(type, language), count);
	}
	private static class Document {

		private final ContainerType type;
		private final String language;

		private Document(ContainerType type, String language) {
			this.language = language;
			this.type = type;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Document that = (Document) o;
			return Objects.equals(language, that.language) &&
				type == that.type;
		}

		@Override
		public int hashCode() {
			return Objects.hash(language, type);
		}

		@Override
		public String toString() {
			return type.getShortName() +
				(language == null
					? ""
					: "-" + language);
		}

		public static Document fromElasticsearch(JsonObject doc) {
			String[] split = doc.getString("_index").split("-");
			return new Document(
				ContainerType.forVersion(split[5]),
				split.length > 6
					? split[6]
					: null
			);
		}
	}
}
