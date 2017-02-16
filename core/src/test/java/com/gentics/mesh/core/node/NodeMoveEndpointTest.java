package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.AbstractRestEndpointTest;

public class NodeMoveEndpointTest extends AbstractRestEndpointTest {

	@Test
	public void testMoveNodeIntoNonFolderNode() {
		try (NoTx noTx = db.noTx()) {
			String releaseUuid = project().getLatestRelease().getUuid();
			Node sourceNode = folder("news");
			Node targetNode = content("concorde");
			String oldParentUuid = sourceNode.getParentNode(releaseUuid).getUuid();
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
			call(() -> client().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid()), BAD_REQUEST,
					"node_move_error_targetnode_is_no_folder");
			assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode(releaseUuid).getUuid());
		}
	}

	@Test
	public void testMoveNodesSame() {
		try (NoTx noTx = db.noTx()) {
			String releaseUuid = project().getLatestRelease().getUuid();
			Node sourceNode = folder("news");
			String oldParentUuid = sourceNode.getParentNode(releaseUuid).getUuid();
			assertNotEquals(sourceNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
			call(() -> client().moveNode(PROJECT_NAME, sourceNode.getUuid(), sourceNode.getUuid()), BAD_REQUEST, "node_move_error_same_nodes");
			assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode(releaseUuid).getUuid());
		}
	}

	@Test
	public void testMoveNodeIntoChildNode() {
		try (NoTx noTx = db.noTx()) {
			String releaseUuid = project().getLatestRelease().getUuid();
			Node sourceNode = folder("news");
			Node targetNode = folder("2015");
			String oldParentUuid = sourceNode.getParentNode(releaseUuid).getUuid();
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());

			call(() -> client().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid()), BAD_REQUEST,
					"node_move_error_not_allowed_to_move_node_into_one_of_its_children");

			assertEquals("The node should not have been moved but it was.", oldParentUuid, sourceNode.getParentNode(releaseUuid).getUuid());
		}
	}

	@Test
	public void testMoveNodeWithoutPerm() {
		try (NoTx noTx = db.noTx()) {
			String releaseUuid = project().getLatestRelease().getUuid();
			Node sourceNode = folder("deals");
			Node targetNode = folder("2015");
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
			role().revokePermissions(sourceNode, GraphPermission.UPDATE_PERM);

			call(() -> client().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid()), FORBIDDEN, "error_missing_perm",
					sourceNode.getUuid());
			assertNotEquals("The source node should not have been moved.", targetNode.getUuid(),
					folder("deals").getParentNode(releaseUuid).getUuid());
		}
	}

	@Test
	public void testMoveNodeWithPerm() {
		try (NoTx noTx = db.noTx()) {
			String releaseUuid = project().getLatestRelease().getUuid();
			Node sourceNode = folder("deals");
			Node targetNode = folder("2015");
			String oldSourceParentId = sourceNode.getParentNode(releaseUuid).getUuid();
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
			call(() -> client().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid()));

			sourceNode.reload();
			try (Tx tx = db.tx()) {
				assertNotEquals("The source node parent uuid should have been updated.", oldSourceParentId,
						sourceNode.getParentNode(releaseUuid).getUuid());
				assertEquals("The source node should have been moved and the target uuid should match the parent node uuid of the source node.",
						targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
				assertEquals("A store event for each language variation per version should occure", 4, dummySearchProvider.getStoreEvents().size());
			}
			// TODO assert entries
		}
	}

	@Test
	public void testMoveNodeWithNoSegmentFieldDefined() {

		try (NoTx noTx = db.noTx()) {

			//1. Create new schema which does not have a segmentfield defined
			SchemaCreateRequest createRequest = new SchemaCreateRequest();
			createRequest.setName("test");
			createRequest.setDescription("Some test schema");
			createRequest.setDisplayField("stringField");
			createRequest.getFields().add(FieldUtil.createStringFieldSchema("stringField"));
			createRequest.validate();
			SchemaResponse schemaResponse = call(() -> client().createSchema(createRequest));

			// 2. Add schema to project
			call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

			// 3. Assign the schema to the initial release
			String releaseUuid = project().getLatestRelease().getUuid();
			SchemaReference reference = new SchemaReference();
			reference.setName("test");
			reference.setVersion(1);
			call(() -> client().assignReleaseSchemaVersions(PROJECT_NAME, releaseUuid, reference));

			// We don't need to wait for a schema migration because there are no nodes which use the schema
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("test"));
			request.getFields().put("stringField", FieldUtil.createStringField("blar"));
			request.setParentNodeUuid(folder("2015").getUuid());
			request.setLanguage("en");
			NodeResponse nodeResponse = call(
					() -> client().createNode(PROJECT_NAME, request, new NodeParameters().setResolveLinks(LinkType.FULL)));
			assertEquals("The node has no segmentfield value and thus a 404 path should be returned.", "/api/v1/dummy/webroot/error/404",
					nodeResponse.getPath());
			assertEquals("The node has no segmentfield value and thus a 404 path should be returned.", "/api/v1/dummy/webroot/error/404",
					nodeResponse.getLanguagePaths().get("en"));

			// 4. Now move the node to folder 2014
			call(() -> client().moveNode(PROJECT_NAME, nodeResponse.getUuid(), folder("2014").getUuid()));
		}

	}

	@Test
	public void testMoveInRelease() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			Node movedNode = folder("deals");
			Node targetNode = folder("2015");

			// 1. Get original parent uuid
			String oldParentUuid = call(() -> client().findNodeByUuid(PROJECT_NAME, movedNode.getUuid(), new VersioningParameters().draft()))
					.getParentNode().getUuid();

			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			NodeResponse migrated = migrateNode(PROJECT_NAME, movedNode.getUuid(), initialRelease.getName(), newRelease.getName());
			assertThat(migrated.getParentNode()).as("Migrated node parent").isNotNull();
			assertThat(migrated.getParentNode().getUuid()).as("Migrated node parent").isEqualTo(oldParentUuid);

			// 2. Move in initial release
			call(() -> client().moveNode(PROJECT_NAME, movedNode.getUuid(), targetNode.getUuid(),
					new VersioningParameters().setRelease(initialRelease.getName())));

			// 3. Assert that the node still uses the old parent for the new release
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, movedNode.getUuid(), new VersioningParameters().draft())).getParentNode()
					.getUuid()).as("Parent Uuid in new release").isEqualTo(oldParentUuid);

			// 4. Assert that the node uses the new parent for the initial release
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, movedNode.getUuid(),
					new VersioningParameters().setRelease(initialRelease.getName()).draft())).getParentNode().getUuid())
							.as("Parent Uuid in initial release").isEqualTo(targetNode.getUuid());
		}
	}
}
