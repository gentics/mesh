package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeMoveEndpointTest extends AbstractMeshTest {

	@Test
	public void testMoveNodeIntoNonFolderNode() {
		try (Tx tx = tx()) {
			String branchUuid = project().getLatestBranch().getUuid();
			Node sourceNode = folder("news");
			Node targetNode = content("concorde");
			String oldParentUuid = sourceNode.getParentNode(branchUuid).getUuid();
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(branchUuid).getUuid());
			call(() -> client().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid()), BAD_REQUEST,
					"node_move_error_targetnode_is_no_folder");
			assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode(branchUuid).getUuid());
		}
	}

	@Test
	public void testMoveNodesSame() {
		try (Tx tx = tx()) {
			String branchUuid = project().getLatestBranch().getUuid();
			Node sourceNode = folder("news");
			String oldParentUuid = sourceNode.getParentNode(branchUuid).getUuid();
			assertNotEquals(sourceNode.getUuid(), sourceNode.getParentNode(branchUuid).getUuid());
			call(() -> client().moveNode(PROJECT_NAME, sourceNode.getUuid(), sourceNode.getUuid()), BAD_REQUEST, "node_move_error_same_nodes");
			assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode(branchUuid).getUuid());
		}
	}

	@Test
	public void testMoveNodeIntoChildNode() {
		try (Tx tx = tx()) {
			String branchUuid = project().getLatestBranch().getUuid();
			Node sourceNode = folder("news");
			Node targetNode = folder("2015");
			String oldParentUuid = sourceNode.getParentNode(branchUuid).getUuid();
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(branchUuid).getUuid());

			call(() -> client().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid()), BAD_REQUEST,
					"node_move_error_not_allowed_to_move_node_into_one_of_its_children");

			assertEquals("The node should not have been moved but it was.", oldParentUuid, sourceNode.getParentNode(branchUuid).getUuid());
		}
	}

	@Test
	public void testMoveNodeWithoutPerm() {
		Node sourceNode = folder("deals");
		Node targetNode = folder("2015");

		try (Tx tx = tx()) {
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(initialBranchUuid()).getUuid());
			role().revokePermissions(sourceNode, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid()), FORBIDDEN, "error_missing_perm",
					sourceNode.getUuid(), UPDATE_PERM.getRestPerm().getName());
			assertNotEquals("The source node should not have been moved.", targetNode.getUuid(),
					folder("deals").getParentNode(initialBranchUuid()).getUuid());
		}
	}

	@Test
	public void testMoveNodeWithPerm() {
		Node sourceNode = folder("deals");
		Node targetNode = folder("2015");
		String branchUuid = initialBranchUuid();
		String sourceNodeUuid = tx(() -> sourceNode.getUuid());
		String targetNodeUuid = tx(() -> targetNode.getUuid());
		String oldSourceParentId = tx(() -> sourceNode.getParentNode(branchUuid).getUuid());
		assertNotEquals(targetNodeUuid, tx(() -> sourceNode.getParentNode(branchUuid).getUuid()));
		call(() -> client().moveNode(PROJECT_NAME, sourceNodeUuid, targetNodeUuid));

		try (Tx tx2 = tx()) {
			assertNotEquals("The source node parent uuid should have been updated.", oldSourceParentId,
					sourceNode.getParentNode(branchUuid).getUuid());
			assertEquals("The source node should have been moved and the target uuid should match the parent node uuid of the source node.",
					targetNode.getUuid(), sourceNode.getParentNode(branchUuid).getUuid());
			assertEquals("A store event for each language variation per version should occure", 4, trackingSearchProvider().getStoreEvents().size());
		}
		// TODO assert entries
	}

	@Test
	public void testMoveNodeWithNoSegmentFieldDefined() {

		try (Tx tx = tx()) {

			// 1. Create new schema which does not have a segmentfield defined
			SchemaCreateRequest createRequest = new SchemaCreateRequest();
			createRequest.setName("test");
			createRequest.setDescription("Some test schema");
			createRequest.setDisplayField("stringField");
			createRequest.getFields().add(FieldUtil.createStringFieldSchema("stringField"));
			createRequest.validate();
			SchemaResponse schemaResponse = call(() -> client().createSchema(createRequest));

			// 2. Add schema to project
			call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

			// 3. Assign the schema to the initial branch
			String branchUuid = project().getLatestBranch().getUuid();
			SchemaReferenceImpl reference = new SchemaReferenceImpl();
			reference.setName("test");
			reference.setVersion("1.0");
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, branchUuid, reference));

			// We don't need to wait for a schema migration because there are no nodes which use the schema
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("test"));
			request.getFields().put("stringField", FieldUtil.createStringField("blar"));
			request.setParentNodeUuid(folder("2015").getUuid());
			request.setLanguage("en");
			NodeResponse nodeResponse = call(
					() -> client().createNode(PROJECT_NAME, request, new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
			assertEquals("The node has no segmentfield value and thus a 404 path should be returned.", "/api/v1/dummy/webroot/error/404",
					nodeResponse.getPath());
			assertEquals("The node has no segmentfield value and thus a 404 path should be returned.", "/api/v1/dummy/webroot/error/404",
					nodeResponse.getLanguagePaths().get("en"));

			// 4. Now move the node to folder 2014
			call(() -> client().moveNode(PROJECT_NAME, nodeResponse.getUuid(), folder("2014").getUuid()));
		}

	}

	@Test
	public void testMoveInBranch() {
		Branch newBranch;
		Project project = project();
		Node movedNode = folder("deals");
		Node targetNode = folder("2015");
		String oldParentUuid;
		try (Tx tx = tx()) {
			// 1. Get original parent uuid
			oldParentUuid = call(() -> client().findNodeByUuid(PROJECT_NAME, movedNode.getUuid(), new VersioningParametersImpl().draft()))
					.getParentNode().getUuid();

			newBranch = project.getBranchRoot().create("newbranch", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse migrated = migrateNode(PROJECT_NAME, movedNode.getUuid(), initialBranch().getName(), newBranch.getName());
			assertThat(migrated.getParentNode()).as("Migrated node parent").isNotNull();
			assertThat(migrated.getParentNode().getUuid()).as("Migrated node parent").isEqualTo(oldParentUuid);

			// 2. Move in initial branch
			call(() -> client().moveNode(PROJECT_NAME, movedNode.getUuid(), targetNode.getUuid(),
					new VersioningParametersImpl().setBranch(initialBranch().getName())));

			// 3. Assert that the node still uses the old parent for the new branch
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, movedNode.getUuid(), new VersioningParametersImpl().draft())).getParentNode()
					.getUuid()).as("Parent Uuid in new branch").isEqualTo(oldParentUuid);

			// 4. Assert that the node uses the new parent for the initial branch
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, movedNode.getUuid(),
					new VersioningParametersImpl().setBranch(initialBranch().getName()).draft())).getParentNode().getUuid())
							.as("Parent Uuid in initial branch").isEqualTo(targetNode.getUuid());
		}
	}

	@Test
	public void moveToOtherLanguage() {
		NodeReference rootNode = getRootNode();
		NodeResponse deFolder = createFolder(rootNode, "de", "deFolder");
		publishNode(deFolder);
		NodeResponse enFolder = createFolder(rootNode, "en", "enFolder");
		publishNode(enFolder);
		moveFolder(enFolder, deFolder);
	}

	private NodeReference getRootNode() {
		return client().findProjectByName(PROJECT_NAME).toSingle()
			.map(ProjectResponse::getRootNode)
			.blockingGet();
	}

	private NodeResponse createFolder(NodeReference parentNode, String language, String name) {
		FieldMapImpl fields = new FieldMapImpl();
		NodeCreateRequest request = new NodeCreateRequest()
			.setParentNode(parentNode)
			.setSchemaName("folder")
			.setLanguage(language)
			.setFields(fields);
		fields.put("name", new StringFieldImpl().setString(name));
		fields.put("slug", new StringFieldImpl().setString(name));
		return client().createNode(PROJECT_NAME, request).toSingle().blockingGet();
	}

	private void publishNode(NodeResponse node) {
		client().publishNode(PROJECT_NAME, node.getUuid()).toCompletable().blockingAwait();
	}

	private void moveFolder(NodeResponse from, NodeResponse to) {
		client().moveNode(PROJECT_NAME, from.getUuid(), to.getUuid()).toCompletable().blockingAwait();
	}
}
