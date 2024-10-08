package com.gentics.mesh.search;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = FULL)
public class NodeSearchEndpointDTest extends AbstractNodeSearchEndpointTest {

	public NodeSearchEndpointDTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testSearchListOfMicronodesResolveLinks() throws Exception {
		addMicronodeListField();
		recreateIndices();

		for (String firstName : Arrays.asList("Mickey", "Donald")) {
			for (String lastName : Arrays.asList("Mouse", "Duck")) {
				// valid names always begin with the same character
				boolean expectResult = firstName.substring(0, 1).equals(lastName.substring(0, 1));

				NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getNestedVCardListSearch(firstName, lastName),
					new PagingParametersImpl().setPage(1).setPerPage(2L), new NodeParametersImpl().setResolveLinks(LinkType.FULL),
					new VersioningParametersImpl().draft()));

				if (expectResult) {
					assertEquals("Check returned search results", 1, response.getData().size());
					assertEquals("Check total search results", 1, response.getMetainfo().getTotalCount());
					for (NodeResponse nodeResponse : response.getData()) {
						assertNotNull("Returned node must not be null", nodeResponse);
						assertEquals("Check result uuid", db().tx(() -> content("concorde").getUuid()), nodeResponse.getUuid());
					}
				} else {
					assertEquals("Check returned search results", 0, response.getData().size());
					assertEquals("Check total search results", 0, response.getMetainfo().getTotalCount());
				}
			}
		}
	}

	@Test
	public void testTrigramSearchQuery() throws Exception {
		recreateIndices();

		// TODO this test should work once the lowercase filter has been applied
		JsonObject query = new JsonObject().put("min_score", 1.0).put("query", new JsonObject().put("match_phrase", new JsonObject().put(
			"fields.content", new JsonObject().put("query", "Hersteller"))));

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query.toString(), new PagingParametersImpl().setPage(1).setPerPage(
			2L), new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages("de")));
		assertEquals(1, response.getData().size());

		String name = response.getData().get(0).getFields().getStringField("teaser").getString();
		assertEquals("Honda NR german", name);
	}

	@Test
	public void testSchemaMigrationNodeSearchTest() throws Exception {
		recreateIndices();

		// 2. Assert that the the en, de variant of the node could be found in the search index
		String uuid = db().tx(() -> content("concorde").getUuid());
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("uuid", uuid), new PagingParametersImpl()
			.setPage(1).setPerPage(10L), new NodeParametersImpl().setLanguages("en", "de"), new VersioningParametersImpl().draft()));
		assertEquals("We expect to find the two language versions.", 2, response.getData().size());

		// 3. Prepare an updated schema
		String schemaUuid;
		SchemaUpdateRequest schema;
		try (Tx tx = tx()) {
			HibNode concorde = content("concorde");
			HibSchemaVersion schemaVersion = concorde.getSchemaContainer().getLatestVersion();
			schema = JsonUtil.readValue(schemaVersion.getJson(), SchemaUpdateRequest.class);
			schema.addField(FieldUtil.createStringFieldSchema("extraField"));
			schemaUuid = concorde.getSchemaContainer().getUuid();
		}
		// Clear the schema storage in order to purge the reference from the storage which we would otherwise modify.
		mesh().serverSchemaStorage().clear();

		// 4. Invoke the schema migration
		GenericMessageResponse message = call(() -> client().updateSchema(schemaUuid, schema, new SchemaUpdateParametersImpl()
			.setUpdateAssignedBranches(false)));
		assertThat(message).matches("schema_updated_migration_deferred", "content", "2.0");

		// 5. Assign the new schema version to the branch
		SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(schemaUuid));

		// Wait for migration to complete
		grantAdmin();
		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, db().tx(() -> project().getLatestBranch().getUuid()),
				new SchemaReferenceImpl().setUuid(updatedSchema.getUuid()).setVersion(updatedSchema.getVersion())));
		}, COMPLETED, 1);
		revokeAdmin();

		waitForSearchIdleEvent();

		// 6. Assert that the two migrated language variations can be found
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("uuid", uuid), new PagingParametersImpl().setPage(1).setPerPage(
			10L), new NodeParametersImpl().setLanguages("en", "de"), new VersioningParametersImpl().draft()));
		assertEquals("We only expect to find the two language versions while searching for uuid {" + uuid + "}", 2, response.getData().size());
	}

	@Test
	public void testSearchManyNodesWithMicronodes() throws Exception {
		long numAdditionalNodes = 99;
		String branchUuid = tx(() -> project().getLatestBranch().getUuid());
		addMicronodeField();
		String english = english();
		NodeReference parentNode = tx(tx -> {
			return tx.nodeDao().getParentNode(content("concorde"), branchUuid).transformToMinimalReference();
		});
		HibSchemaVersion schemaVersion = tx(() -> content("concorde").getSchemaContainer().getLatestVersion());
		NodeResponse request = call(() -> client().findNodeByUuid(projectName(), content("concorde").getUuid()));
		for (int i = 0; i < numAdditionalNodes; i++) {
			NodeCreateRequest createRequest = new NodeCreateRequest();
			createRequest.setSchema(tx(() -> schemaVersion.transformToReference()));
			MicronodeResponse micronodeField = new MicronodeResponse();
			micronodeField.getFields().putAll(
					Map.of("firstName", new StringFieldImpl().setString("Mickey"),
							"lastName", new StringFieldImpl().setString("Mouse"))
					);
			micronodeField.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
			FieldMap fieldMap = request.getFields().putAll(
					Map.of("slug", new StringFieldImpl().setString("Name_" + i), "vcard", micronodeField)
					);
			createRequest.setFields(fieldMap);
			createRequest.setParentNode(parentNode);
			createRequest.setLanguage(english);

			call(() -> client().createNode(projectName(), createRequest));
		}
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.vcard.fields-vcard.firstName", "Mickey"),
			new PagingParametersImpl().setPage(1).setPerPage(numAdditionalNodes + 1), new VersioningParametersImpl().draft()));

		assertEquals("Check returned search results", numAdditionalNodes + 1, response.getData().size());

	}

	/**
	 * Tests if all tags are in the node response when searching for a node.
	 *
	 * @throws JSONException
	 * @throws InterruptedException
	 */
	@Test
	public void testTagCount() throws Exception {
		recreateIndices();

		HibNode node = tx(() -> content("concorde"));

		long previousTagCount = tx(tx -> {
			TagDao tagDao = tx.tagDao();
			return tagDao.getTags(node, project().getLatestBranch()).count();
		});

		// Create tags:
		int tagCount = 20;
		for (int i = 0; i < tagCount; i++) {
			TagResponse tagResponse = createTag(PROJECT_NAME, tx(() -> tagFamily("colors").getUuid()), "tag" + i);
			// Add tags to node:
			call(() -> client().addTagToNode(PROJECT_NAME, node.getUuid(), tagResponse.getUuid(), new VersioningParametersImpl().draft()));
		}

		waitForSearchIdleEvent();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "Concorde"),
			new VersioningParametersImpl().draft()));
		assertEquals("Expect to only get one search result", 1, response.getMetainfo().getTotalCount());

		// assert tag count
		long nColorTags = response.getData().get(0).getTags().stream().filter(ref -> ref.getTagFamily().equals("colors")).count();
		long nBasicTags = response.getData().get(0).getTags().stream().filter(ref -> ref.getTagFamily().equals("basic")).count();
		assertEquals("Expect correct tag count", previousTagCount + tagCount, nColorTags + nBasicTags);
	}

	@Test
	public void testGlobalNodeSearch() throws Exception {
		recreateIndices();

		NodeResponse oldNode = call(() -> client().findNodeByUuid(PROJECT_NAME, tx(() -> content("concorde").getUuid()),
			new VersioningParametersImpl().draft()));

		// Create a new project with a new node
		ProjectCreateRequest createProject = new ProjectCreateRequest();
		createProject.setSchema(new SchemaReferenceImpl().setName("content"));
		createProject.setName("mynewproject");
		ProjectResponse projectResponse = call(() -> client().createProject(createProject));

		NodeCreateRequest createNode = new NodeCreateRequest();
		createNode.setLanguage("en");
		createNode.setSchema(new SchemaReferenceImpl().setName("content"));
		createNode.setParentNode(projectResponse.getRootNode());
		createNode.getFields().put("slug", FieldUtil.createStringField("newNode"));
		createNode.getFields().put("teaser", FieldUtil.createStringField("newTeaser"));
		createNode.getFields().put("content", FieldUtil.createHtmlField("Concorde"));
		NodeResponse newNode = call(() -> client().createNode("mynewproject", createNode));

		waitForSearchIdleEvent();

		// search in old project
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "Concorde"),
			new VersioningParametersImpl().draft()));
		assertThat(response.getData()).as("Search result in " + PROJECT_NAME).usingElementComparatorOnFields("uuid").containsOnly(oldNode);

		// search in new project
		response = call(() -> client().searchNodes("mynewproject", getSimpleQuery("fields.content", "Concorde"), new VersioningParametersImpl()
			.draft()));
		assertThat(response.getData()).as("Search result in mynewproject").usingElementComparatorOnFields("uuid").containsOnly(newNode);

		// search globally
		response = call(() -> client().searchNodes(getSimpleQuery("fields.content", "Concorde"), new VersioningParametersImpl().draft()));
		assertThat(response.getData()).as("Global search result").usingElementComparatorOnFields("uuid").containsOnly(newNode, oldNode);

	}

	@Test
	public void testTakeDraftOffline() throws Exception {
		recreateIndices();

		// 1. Create a new project and a folder schema
		ProjectCreateRequest createProject = new ProjectCreateRequest();
		createProject.setName("mynewproject");
		createProject.setSchema(new SchemaReferenceImpl().setName("folder"));
		ProjectResponse projectResponse = call(() -> client().createProject(createProject));

		// 2. Create a new node in the base of the project
		NodeCreateRequest createNode = new NodeCreateRequest();
		createNode.setLanguage("en");
		createNode.setSchema(new SchemaReferenceImpl().setName("folder"));
		createNode.setParentNode(projectResponse.getRootNode());
		createNode.getFields().put("name", FieldUtil.createStringField("AwesomeString"));
		NodeResponse newNode = call(() -> client().createNode("mynewproject", createNode));

		waitForSearchIdleEvent();

		// 3. Search globally for published version - The created node is still a draft and thus can't be found
		NodeListResponse response = call(() -> client().searchNodes(getSimpleQuery("fields.name", "AwesomeString"), new VersioningParametersImpl()
			.setVersion("published")));
		assertThat(response.getData()).as("Global search result before publishing").isEmpty();

		// 4. Search globally for draft version - The created node should be found since it is a draft
		response = call(() -> client().searchNodes(getSimpleQuery("fields.name", "AwesomeString"), new VersioningParametersImpl().setVersion(
			"draft")));
		assertThat(response.getData()).as("Global search result after publishing").usingElementComparatorOnFields("uuid").containsOnly(newNode);

		// 5. Invoke the take offline action on the project base node
		String baseUuid = db().tx(() -> project().getBaseNode().getUuid());
		call(() -> client().takeNodeOffline(PROJECT_NAME, baseUuid, new PublishParametersImpl().setRecursive(true)));

		waitForSearchIdleEvent();

		// 6. The node should still be found because it is still a draft
		response = call(() -> client().searchNodes(getSimpleQuery("fields.name", "AwesomeString"), new VersioningParametersImpl().setVersion(
			"draft")));
		assertThat(response.getData()).as("Global search result after publishing").usingElementComparatorOnFields("uuid").containsOnly(newNode);

		// 7. Search globally for the published version - Still there is no published version of the node
		response = call(() -> client().searchNodes(getSimpleQuery("fields.name", "AwesomeString"), new VersioningParametersImpl().setVersion(
			"published")));
		assertThat(response.getData()).as("Global search result before publishing").isEmpty();

	}

	@Test
	public void testGlobalPublishedNodeSearch() throws Exception {
		recreateIndices();

		// 1. Create a new project and a folder schema
		ProjectCreateRequest createProject = new ProjectCreateRequest();
		createProject.setName("mynewproject");
		createProject.setSchema(new SchemaReferenceImpl().setName("folder"));
		ProjectResponse projectResponse = call(() -> client().createProject(createProject));

		// 2. Create a new node in the base of the project
		NodeCreateRequest createNode = new NodeCreateRequest();
		createNode.setLanguage("en");
		createNode.setSchema(new SchemaReferenceImpl().setName("folder"));
		createNode.setParentNodeUuid(projectResponse.getRootNode().getUuid());
		createNode.getFields().put("name", FieldUtil.createStringField("AwesomeString"));
		NodeResponse newNode = call(() -> client().createNode("mynewproject", createNode));

		waitForSearchIdleEvent();

		// 3. search globally for published version - The created node is still a draft and thus can't be found
		NodeListResponse response = call(() -> client().searchNodes(getSimpleQuery("fields.name", "AwesomeString"), new VersioningParametersImpl()
			.setVersion("published")));
		assertThat(response.getData()).as("Global search result before publishing").isEmpty();

		// 4. now publish the node
		call(() -> client().publishNode("mynewproject", newNode.getUuid()));

		waitForSearchIdleEvent();

		// 5. search globally for published version - by default published nodes will be searched for
		response = call(() -> client().searchNodes(getSimpleQuery("fields.name", "AwesomeString"), new VersioningParametersImpl().setVersion(
			"published")));
		assertThat(response.getData()).as("Global search result after publishing").usingElementComparatorOnFields("uuid").containsOnly(newNode);

		// 6. Invoke the take offline action on the project base node
		String baseUuid = db().tx(() -> project().getBaseNode().getUuid());
		call(() -> client().takeNodeOffline(PROJECT_NAME, baseUuid, new PublishParametersImpl().setRecursive(true)));

		waitForSearchIdleEvent();

		// 7. search globally for published version and assert that the node could be found
		response = call(() -> client().searchNodes(getSimpleQuery("fields.name", "AwesomeString")));
		assertThat(response.getData()).as("Global search result after publishing").usingElementComparatorOnFields("uuid").containsOnly(newNode);
	}

}
