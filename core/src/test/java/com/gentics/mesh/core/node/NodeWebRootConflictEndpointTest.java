package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractRestEndpointTest;

public class NodeWebRootConflictEndpointTest extends AbstractRestEndpointTest {

	/**
	 * Create two published nodes and move the second node into the folder of the first node. A conflict should occur since the node published segment path
	 * conflict with each other.
	 */
	@Test
	public void testDuplicateDueMove() {

		String conflictingName = "conflictName";
		try (Tx trx = db.tx()) {
			Node folderA = folder("2014");
			// 1. Create nodeA
			NodeCreateRequest requestA = new NodeCreateRequest();
			requestA.setLanguage("en");
			requestA.setParentNodeUuid(folderA.getUuid());
			requestA.setSchema(new SchemaReference().setName("content"));
			requestA.getFields().put("name", FieldUtil.createStringField("nodeA"));
			requestA.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			NodeResponse nodeA = call(() -> client().createNode(PROJECT_NAME, requestA));

			// 2. Publish nodeA
			call(() -> client().publishNode(PROJECT_NAME, nodeA.getUuid()));

			// 3. Create nodeB
			Node folderB = folder("2015");
			NodeCreateRequest requestB = new NodeCreateRequest();
			requestB.setLanguage("en");
			requestB.setParentNodeUuid(folderB.getUuid());
			requestB.setSchema(new SchemaReference().setName("content"));
			requestB.getFields().put("name", FieldUtil.createStringField("nodeB"));
			requestB.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			NodeResponse nodeB = call(() -> client().createNode(PROJECT_NAME, requestB));

			// 4. Publish nodeB
			call(() -> client().publishNode(PROJECT_NAME, nodeB.getUuid()));

			// 5. Update node b to create a draft which would not conflict with node a
			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
			nodeUpdateRequest.setVersion(nodeB.getVersion());
			nodeUpdateRequest.getFields().put("filename", FieldUtil.createStringField("nodeB"));
			nodeUpdateRequest.setLanguage("en");
			call(() -> client().updateNode(PROJECT_NAME, nodeB.getUuid(), nodeUpdateRequest));

			// 6. Move Node B into FolderA
			call(() -> client().moveNode(PROJECT_NAME, nodeB.getUuid(), folderA.getUuid()), CONFLICT, "node_conflicting_segmentfield_move",
					"filename", conflictingName);

		}
	}

	@Test
	public void testCreateDuplicateWebrootPath() {
		String conflictingName = "filename.html";
		Node parent = db.noTx(() -> folder("2015"));
		SchemaContainer contentSchema = db.noTx(() -> schemaContainer("content"));

		db.noTx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, create).invoke();
			latchFor(future);
			assertSuccess(future);

			// try to create the new content with same filename
			create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some other title"));
			create.getFields().put("name", FieldUtil.createStringField("some other name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			future = client().createNode(PROJECT_NAME, create).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "node_conflicting_segmentfield_update", "filename", conflictingName);
			return null;
		});
	}

	@Test
	public void testUpdateDuplicateWebrootPath() {
		String conflictingName = "filename.html";
		String nonConflictingName = "otherfilename.html";
		Node parent = db.noTx(() -> folder("2015"));
		SchemaContainer contentSchema = db.noTx(() -> schemaContainer("content"));

		db.noTx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, create).invoke();
			latchFor(future);
			assertSuccess(future);

			// create a new content
			create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(nonConflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			future = client().createNode(PROJECT_NAME, create).invoke();
			latchFor(future);
			assertSuccess(future);
			String uuid = future.result().getUuid();

			// try to update with conflict
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion(new VersionReference(null, "0.1"));
			update.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			future = client().updateNode(PROJECT_NAME, uuid, update).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "node_conflicting_segmentfield_update", "filename", conflictingName);
			return null;
		});
	}

	@Test
	public void testTranslateDuplicateWebrootPath() {
		String conflictingName = "filename.html";
		Node parent = db.noTx(() -> folder("2015"));
		SchemaContainer contentSchema = db.noTx(() -> schemaContainer("content"));

		db.noTx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, create).invoke();
			latchFor(future);
			assertSuccess(future);
			String uuid = future.result().getUuid();

			// translate the content
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("title", FieldUtil.createStringField("Irgendein Titel"));
			update.getFields().put("name", FieldUtil.createStringField("Irgendein Name"));
			update.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			update.getFields().put("content", FieldUtil.createStringField("Gesegnete Mahlzeit!"));
			future = client().updateNode(PROJECT_NAME, uuid, update).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "node_conflicting_segmentfield_update", "filename", conflictingName);
			return null;
		});
	}

	@Test
	public void testMoveDuplicateWebrootPath() {
		String conflictingName = "filename.html";

		Node parent = db.noTx(() -> folder("2015"));
		Node otherParent = db.noTx(() -> folder("news"));
		SchemaContainer contentSchema = db.noTx(() -> schemaContainer("content"));
		db.noTx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, create));
			String uuid = response.getUuid();

			// create a "conflicting" content in another folder
			NodeCreateRequest create2 = new NodeCreateRequest();
			create2.setParentNodeUuid(otherParent.getUuid());
			create2.setLanguage("en");
			create2.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create2.getFields().put("title", FieldUtil.createStringField("some other title"));
			create2.getFields().put("name", FieldUtil.createStringField("some other name"));
			create2.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			call(() -> client().createNode(PROJECT_NAME, create2));

			// try to move the original node
			call(() -> client().moveNode(PROJECT_NAME, uuid, otherParent.getUuid()), CONFLICT, "node_conflicting_segmentfield_move", "filename",
					conflictingName);
			return null;
		});
	}

	@Test
	public void testDuplicateCrossReleases() {

		String conflictingName = "filename.html";
		String newReleaseName = "newrelease";
		SchemaContainer contentSchema = db.noTx(() -> {
			return schemaContainer("content");
		});
		// 1. Create new release and migrate nodes
		db.noTx(() -> {
			Release newRelease = project().getReleaseRoot().create(newReleaseName, user());
			meshDagger.nodeMigrationHandler().migrateNodes(newRelease);
			return null;
		});

		// 2. Create content in new release
		db.noTx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			call(() -> client().createNode(PROJECT_NAME, create));

			return null;
		});

		// 3. Create "conflicting" content in initial release
		db.noTx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			call(() -> client().createNode(PROJECT_NAME, create, new VersioningParameters().setRelease(project().getInitialRelease().getUuid())));

			return null;
		});
	}

	@Test
	public void testDuplicateWithOldVersion() {
		String conflictingName = "filename.html";
		String newName = "changed.html";

		SchemaContainer contentSchema = db.noTx(() -> {
			return schemaContainer("content");
		});

		// 1. Create initial content
		String nodeUuid = db.noTx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
		});

		// 2. Modify initial content
		db.noTx(() -> {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion(new VersionReference(null, "0.1"));
			update.getFields().put("filename", FieldUtil.createStringField(newName));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));
			return null;
		});

		// 3. Create "conflicting" content
		db.noTx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
		});
	}

	@Test
	public void testDuplicateWithDrafts() {
		String initialName = "filename.html";
		String conflictingName = "changed.html";
		SchemaContainer contentSchema = db.noTx(() -> {
			return schemaContainer("content");
		});

		// 1. Create and publish initial content
		String nodeUuid = db.noTx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(initialName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			String createdUuid = call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
			return createdUuid;
		});

		// 2. Modify initial content
		db.noTx(() -> {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion(new VersionReference(null, "0.1"));
			update.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));
			return null;
		});

		// 3. Create content. The filename should not cause a conflict since the other node was just updated.
		String otherNodeUuid = db.noTx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(initialName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
		});

		// 4. Modify the second node in order to cause a conflict
		db.noTx(() -> {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion(new VersionReference(null, "0.1"));
			update.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			call(() -> client().updateNode(PROJECT_NAME, otherNodeUuid, update), CONFLICT, "node_conflicting_segmentfield_update", "filename",
					conflictingName);
			return null;
		});

	}

	@Test
	public void testDuplicateWithPublished() {
		String conflictingName = "filename.html";
		String newName = "changed.html";

		SchemaContainer contentSchema = db.noTx(() -> {
			return schemaContainer("content");
		});

		// 1. Create and publish initial content
		String nodeUuid = db.noTx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			String createdUuid = call(() -> client().createNode(PROJECT_NAME, create)).getUuid();

			call(() -> client().publishNode(PROJECT_NAME, createdUuid));

			return createdUuid;
		});

		// 2. Modify initial content
		db.noTx(() -> {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion(new VersionReference(null, "0.1"));
			update.getFields().put("filename", FieldUtil.createStringField(newName));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));
			return null;
		});

		// 3. Create conflicting content
		String otherNodeUuid = db.noTx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReference().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("name", FieldUtil.createStringField("some name"));
			create.getFields().put("filename", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
		});

		// 4. Publish conflicting content
		db.noTx(() -> {
			call(() -> client().publishNode(PROJECT_NAME, otherNodeUuid), CONFLICT, "node_conflicting_segmentfield_publish", "filename",
					conflictingName);

			return null;
		});
	}
}
