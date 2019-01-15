package com.gentics.mesh.core.node;

import static com.gentics.mesh.FieldUtil.createStringField;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeWebRootConflictEndpointTest extends AbstractMeshTest {

	/**
	 * Create two published nodes and move the second node into the folder of the first node. A conflict should occur since the node published segment path
	 * conflict with each other.
	 */
	@Test
	public void testDuplicateDueMove() {

		String conflictingName = "conflictName";
		try (Tx trx = tx()) {
			Node folderA = folder("2014");
			// 1. Create nodeA
			NodeCreateRequest requestA = new NodeCreateRequest();
			requestA.setLanguage("en");
			requestA.setParentNodeUuid(folderA.getUuid());
			requestA.setSchema(new SchemaReferenceImpl().setName("content"));
			requestA.getFields().put("teaser", FieldUtil.createStringField("nodeA"));
			requestA.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			NodeResponse nodeA = call(() -> client().createNode(PROJECT_NAME, requestA));

			// 2. Publish nodeA
			call(() -> client().publishNode(PROJECT_NAME, nodeA.getUuid()));

			// 3. Create nodeB
			Node folderB = folder("2015");
			NodeCreateRequest requestB = new NodeCreateRequest();
			requestB.setLanguage("en");
			requestB.setParentNodeUuid(folderB.getUuid());
			requestB.setSchema(new SchemaReferenceImpl().setName("content"));
			requestB.getFields().put("teaser", FieldUtil.createStringField("nodeB"));
			requestB.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			NodeResponse nodeB = call(() -> client().createNode(PROJECT_NAME, requestB));

			// 4. Publish nodeB
			call(() -> client().publishNode(PROJECT_NAME, nodeB.getUuid()));

			// 5. Update node b to create a draft which would not conflict with node a
			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
			nodeUpdateRequest.setVersion(nodeB.getVersion());
			nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("nodeB"));
			nodeUpdateRequest.setLanguage("en");
			call(() -> client().updateNode(PROJECT_NAME, nodeB.getUuid(), nodeUpdateRequest));

			// 6. Move Node B into FolderA
			call(() -> client().moveNode(PROJECT_NAME, nodeB.getUuid(), folderA.getUuid()), CONFLICT, "node_conflicting_segmentfield_move",
					"slug", conflictingName);

		}
	}

	@Test
	public void testCreateDuplicateWebrootPath() {
		String conflictingName = "filename.html";
		Node parent = db().tx(() -> folder("2015"));
		SchemaContainer contentSchema = db().tx(() -> schemaContainer("content"));

		db().tx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", createStringField("some title"));
			create.getFields().put("teaser", createStringField("some name"));
			create.getFields().put("slug", createStringField(conflictingName));
			create.getFields().put("content", createStringField("Blessed mealtime!"));
			NodeResponse response = client().createNode(PROJECT_NAME, create).blockingGet();

			// try to create the new content with same slug
			NodeCreateRequest create2 = new NodeCreateRequest();
			create2.setParentNodeUuid(parent.getUuid());
			create2.setLanguage("en");
			create2.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create2.getFields().put("title", createStringField("some other title"));
			create2.getFields().put("teaser", createStringField("some other name"));
			create2.getFields().put("slug", createStringField(conflictingName));
			create2.getFields().put("content", createStringField("Blessed mealtime again!"));
			call(() -> client().createNode(PROJECT_NAME, create2), CONFLICT, "node_conflicting_segmentfield_update", "slug", conflictingName);
			return null;
		});
	}

	@Test
	public void testUpdateDuplicateWebrootPath() {
		String conflictingName = "filename.html";
		String nonConflictingName = "otherfilename.html";
		Node parent = db().tx(() -> folder("2015"));
		SchemaContainer contentSchema = db().tx(() -> schemaContainer("content"));

		db().tx(() -> {
			// create the initial content
			final NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some name"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, create));

			// create a new content
			NodeCreateRequest create2 = new NodeCreateRequest();
			create2.setParentNodeUuid(parent.getUuid());
			create2.setLanguage("en");
			create2.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create2.getFields().put("title", FieldUtil.createStringField("some title"));
			create2.getFields().put("teaser", FieldUtil.createStringField("some name"));
			create2.getFields().put("slug", FieldUtil.createStringField(nonConflictingName));
			create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			response = call(() -> client().createNode(PROJECT_NAME, create2));
			String uuid = response.getUuid();

			// try to update with conflict
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion("0.1");
			update.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			call(() -> client().updateNode(PROJECT_NAME, uuid, update), CONFLICT, "node_conflicting_segmentfield_update", "slug", conflictingName);
			return null;
		});
	}

	@Test
	public void testTranslateDuplicateWebrootPath() {
		String conflictingName = "filename.html";
		Node parent = db().tx(() -> folder("2015"));
		SchemaContainer contentSchema = db().tx(() -> schemaContainer("content"));

		db().tx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			String uuid = client().createNode(PROJECT_NAME, create).toSingle().blockingGet().getUuid();

			// translate the content
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("title", FieldUtil.createStringField("Irgendein Titel"));
			update.getFields().put("teaser", FieldUtil.createStringField("Irgendein teaser"));
			update.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			update.getFields().put("content", FieldUtil.createStringField("Gesegnete Mahlzeit!"));

			call(() -> client().updateNode(PROJECT_NAME, uuid, update), CONFLICT, "node_conflicting_segmentfield_update", "slug", conflictingName);
			return null;
		});
	}

	@Test
	public void testMoveDuplicateWebrootPath() {
		String conflictingName = "filename.html";

		Node parent = db().tx(() -> folder("2015"));
		Node otherParent = db().tx(() -> folder("news"));
		SchemaContainer contentSchema = db().tx(() -> schemaContainer("content"));
		db().tx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, create));
			String uuid = response.getUuid();

			// create a "conflicting" content in another folder
			NodeCreateRequest create2 = new NodeCreateRequest();
			create2.setParentNodeUuid(otherParent.getUuid());
			create2.setLanguage("en");
			create2.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create2.getFields().put("title", FieldUtil.createStringField("some other title"));
			create2.getFields().put("teaser", FieldUtil.createStringField("some other teaser"));
			create2.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			call(() -> client().createNode(PROJECT_NAME, create2));

			// try to move the original node
			call(() -> client().moveNode(PROJECT_NAME, uuid, otherParent.getUuid()), CONFLICT, "node_conflicting_segmentfield_move", "slug",
					conflictingName);
			return null;
		});
	}

	@Test
	public void testDuplicateCrossBranches() {

		String conflictingName = "filename.html";
		String newBranchName = "newbranch";
		SchemaContainer contentSchema = db().tx(() -> {
			return schemaContainer("content");
		});
		// 1. Create new branch and migrate nodes
		db().tx(() -> {
			Branch newBranch = project().getBranchRoot().create(newBranchName, user());
			meshDagger().branchMigrationHandler().migrateBranch(newBranch, null).blockingAwait();
			return null;
		});

		// 2. Create content in new branch
		db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			call(() -> client().createNode(PROJECT_NAME, create));

			return null;
		});

		// 3. Create "conflicting" content in initial branch
		db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			call(() -> client().createNode(PROJECT_NAME, create, new VersioningParametersImpl().setBranch(project().getInitialBranch().getUuid())));

			return null;
		});
	}

	@Test
	public void testDuplicateCrossBranchesSameNode() {
		String conflictingName = "filename.html";
		String newBranchName = "newbranch";
		SchemaContainer contentSchema = db().tx(() -> {
			return schemaContainer("content");
		});
		// 1. Create new branch and migrate nodes
		db().tx(() -> {
			Branch newBranch = project().getBranchRoot().create(newBranchName, user());
			meshDagger().branchMigrationHandler().migrateBranch(newBranch, null).blockingAwait();
			return null;
		});

		// 2. Create content in new branch
		NodeResponse response = db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(PROJECT_NAME, create));
		});

		// 3. Create "conflicting" content in initial branch
		db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			call(() -> client().createNode(response.getUuid(), PROJECT_NAME, create, new VersioningParametersImpl().setBranch(project().getInitialBranch().getUuid())));

			return null;
		});
	}

	@Test
	public void testDuplicateCrossBranchesSameNode1() {
		String conflictingName = "filename.html";
		String newBranchName = "newbranch";
		SchemaContainer contentSchema = db().tx(() -> {
			return schemaContainer("content");
		});
		// 1. Create new branch and migrate nodes
		db().tx(() -> {
			Branch newBranch = project().getBranchRoot().create(newBranchName, user());
			meshDagger().branchMigrationHandler().migrateBranch(newBranch, null).blockingAwait();
			return null;
		});

		// 2. Create "conflicting" content in initial branch
		NodeResponse response = db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));

			return call(() -> client().createNode(PROJECT_NAME, create, new VersioningParametersImpl().setBranch(project().getInitialBranch().getUuid())));
		});

		// 3. Create content in new branch
		db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(response.getUuid(), PROJECT_NAME, create));
		});
	}

	@Test
	public void testDuplicateWithOldVersion() {
		String conflictingName = "filename.html";
		String newName = "changed.html";

		SchemaContainer contentSchema = db().tx(() -> {
			return schemaContainer("content");
		});

		// 1. Create initial content
		String nodeUuid = db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
		});

		// 2. Modify initial content
		db().tx(() -> {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion("0.1");
			update.getFields().put("slug", FieldUtil.createStringField(newName));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));
			return null;
		});

		// 3. Create "conflicting" content
		db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
		});
	}

	@Test
	public void testDuplicateWithDrafts() {
		String initialName = "filename.html";
		String conflictingName = "changed.html";
		SchemaContainer contentSchema = db().tx(() -> {
			return schemaContainer("content");
		});

		// 1. Create and publish initial content
		String nodeUuid = db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(initialName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			String createdUuid = call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
			return createdUuid;
		});

		// 2. Modify initial content
		db().tx(() -> {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion("0.1");
			update.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));
			return null;
		});

		// 3. Create content. The filename should not cause a conflict since the other node was just updated.
		String otherNodeUuid = db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(initialName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
		});

		// 4. Modify the second node in order to cause a conflict
		db().tx(() -> {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion("0.1");
			update.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			call(() -> client().updateNode(PROJECT_NAME, otherNodeUuid, update), CONFLICT, "node_conflicting_segmentfield_update", "slug",
					conflictingName);
			return null;
		});

	}

	@Test
	public void testDuplicateWithPublished() {
		String conflictingName = "filename.html";
		String newName = "changed.html";

		SchemaContainer contentSchema = db().tx(() -> {
			return schemaContainer("content");
		});

		// 1. Create and publish initial content
		String nodeUuid = db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			String createdUuid = call(() -> client().createNode(PROJECT_NAME, create)).getUuid();

			call(() -> client().publishNode(PROJECT_NAME, createdUuid));

			return createdUuid;
		});

		// 2. Modify initial content
		db().tx(() -> {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion("0.1");
			update.getFields().put("slug", FieldUtil.createStringField(newName));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));
			return null;
		});

		// 3. Create conflicting content
		String otherNodeUuid = db().tx(() -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folder("2015").getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", FieldUtil.createStringField("some title"));
			create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
			create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
			return call(() -> client().createNode(PROJECT_NAME, create)).getUuid();
		});

		// 4. Publish conflicting content
		db().tx(() -> {
			call(() -> client().publishNode(PROJECT_NAME, otherNodeUuid), CONFLICT, "node_conflicting_segmentfield_publish", "slug",
					conflictingName);

			return null;
		});
	}
}
