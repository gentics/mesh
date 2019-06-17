package com.gentics.mesh.search;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
@MeshTestSetting(elasticsearch = CONTAINER, testSize = FULL, startServer = true)
public class NodeSearchEndpointGTest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testSearchDraftNodes() throws Exception {
		recreateIndices();

		String oldContent = "supersonic";
		String newContent = "urschnell";
		String uuid = db().tx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent),
				new VersioningParametersImpl().draft()));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newContent), new VersioningParametersImpl()
				.draft()));
		assertThat(response.getData()).as("Search result").isEmpty();

		// change draft version of content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newContent));
		update.setVersion("1.0");
		call(() -> client().updateNode(PROJECT_NAME, concorde.getUuid(), update));

		waitForSearchIdleEvent();

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent), new VersioningParametersImpl()
				.draft()));
		assertThat(response.getData()).as("Search result").isEmpty();

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newContent), new VersioningParametersImpl()
				.draft()));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	/**
	 * Test creating a microschema and adding it to the content schema. Assert that the search endpoint works as expected.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMicronodeMigrationSearch() throws Exception {
		recreateIndices();

		// Assert initial condition
		NodeListResponse response1 = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "supersonic"),
				new VersioningParametersImpl().draft()));
		assertThat(response1.getData()).as("Search result").isNotEmpty();

		String contentUuid = db().tx(() -> content().getUuid());
		String folderUuid = db().tx(() -> folder("2015").getUuid());
		String schemaUuid = db().tx(() -> schemaContainer("content").getUuid());
		SchemaUpdateRequest schemaUpdate = db().tx(() -> JsonUtil.readValue(schemaContainer("content").getLatestVersion().getJson(),
				SchemaUpdateRequest.class));

		// 1. Create the microschema
		MicroschemaCreateRequest microschemaRequest = new MicroschemaCreateRequest();
		microschemaRequest.setName("TestMicroschema");
		microschemaRequest.addField(FieldUtil.createStringFieldSchema("text"));
		microschemaRequest.addField(FieldUtil.createNodeFieldSchema("nodeRef").setAllowedSchemas("content"));
		MicroschemaResponse microschemaResponse = call(() -> client().createMicroschema(microschemaRequest));
		String microschemaUuid = microschemaResponse.getUuid();
		// Assigning the microschema to the project is not needed since this is done during schema update
		// call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschemaUuid));

		// 2. Add micronode field to content schema
		schemaUpdate.addField(FieldUtil.createMicronodeFieldSchema("micro").setAllowedMicroSchemas("TestMicroschema"));

		// Trigger the migration
		tx(() -> group().addRole(roles().get("admin")));
		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, schemaUpdate));
		}, COMPLETED, 1);
		tx(() -> group().removeRole(roles().get("admin")));

		waitForSearchIdleEvent();

		// Assert that the nodes were migrated and added to the new index. The data should be searchable
		NodeListResponse response2 = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "supersonic"),
				new VersioningParametersImpl().draft()));
		assertThat(response2.getData()).as("Search result").isNotEmpty();

		// Lets create a new node that has a micronode
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNode(new NodeReference().setUuid(folderUuid));
		nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("content"));
		nodeCreateRequest.getFields().put("title", FieldUtil.createStringField("someTitle"));
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("someTeaser"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("someSlug"));
		MicronodeResponse micronodeField = new MicronodeResponse();
		micronodeField.setMicroschema(new MicroschemaReferenceImpl().setName("TestMicroschema"));
		micronodeField.getFields().put("text", FieldUtil.createStringField("someText"));
		micronodeField.getFields().put("nodeRef", FieldUtil.createNodeField(contentUuid));
		nodeCreateRequest.getFields().put("micro", micronodeField);
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		assertEquals("someText", nodeResponse.getFields().getMicronodeField("micro").getFields().getStringField("text").getString());

		// 5. Update the microschema
		MicroschemaUpdateRequest microschemaUpdate = new MicroschemaUpdateRequest();
		microschemaUpdate.setName("TestMicroschema");
		microschemaUpdate.addField(FieldUtil.createStringFieldSchema("textNew"));
		microschemaUpdate.addField(FieldUtil.createNodeFieldSchema("nodeRefNew").setAllowedSchemas("content"));

		tx(() -> group().addRole(roles().get("admin")));
		waitForJobs(() -> {
			call(() -> client().updateMicroschema(microschemaUuid, microschemaUpdate));
		}, COMPLETED, 1);
		tx(() -> group().removeRole(roles().get("admin")));

		// Update the node and populate the new fields
		NodeUpdateRequest updateRequest = new NodeUpdateRequest();
		updateRequest.setLanguage("en");
		// The migration bumped the version to 0.2
		updateRequest.setVersion("0.2");
		micronodeField = new MicronodeResponse();
		micronodeField.setMicroschema(new MicroschemaReferenceImpl().setName("TestMicroschema"));
		micronodeField.getFields().put("textNew", FieldUtil.createStringField("someNewText"));
		micronodeField.getFields().put("nodeRefNew", FieldUtil.createNodeField(contentUuid));
		updateRequest.getFields().put("micro", micronodeField);
		call(() -> client().updateNode(PROJECT_NAME, nodeResponse.getUuid(), updateRequest));

		waitForSearchIdleEvent();

		// Verify that the micronode has been migrated
		NodeResponse nodeResponse2 = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeResponse.getUuid()));
		assertEquals("someNewText", nodeResponse2.getFields().getMicronodeField("micro").getFields().getStringField("textNew").getString());

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "supersonic"),
				new VersioningParametersImpl().draft()));
		assertThat(response.getData()).as("Search result").isNotEmpty();

	}

	@Test
	public void testSearchPublishedInBranch() throws Exception {
		grantAdminRole();
		recreateIndices();

		String uuid = tx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
		call(() -> client().publishNode(PROJECT_NAME, uuid));

		// Create a new branch and migrate the nodes
		String branchName = "newbranch";
		BranchCreateRequest createBranch = new BranchCreateRequest();
		createBranch.setName(branchName);
		waitForLatestJob(() -> call(() -> client().createBranch(PROJECT_NAME, createBranch)));

		waitForSearchIdleEvent();

		// Assert that the node can be found within the publish index within the new branch
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "supersonic"),
				new VersioningParametersImpl().setBranch(branchName).setVersion("published")));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	@Test
	public void testSearchTagFamilies() throws Exception {
		recreateIndices();

		String query = getESText("tagFamilySearch.es");

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query));
		assertThat(response.getData()).isNotEmpty();

		for (NodeResponse node : response.getData()) {
			long count = node.getTags().stream().filter(tag -> tag.getName().equals("red")).count();
			assertThat(count).as("The node should have the tag 'red'.").isGreaterThanOrEqualTo(1);
		}
	}

}
