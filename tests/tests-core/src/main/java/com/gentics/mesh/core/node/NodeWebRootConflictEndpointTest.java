package com.gentics.mesh.core.node;

import static com.gentics.mesh.FieldUtil.createStringField;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.io.ByteArrayInputStream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.impl.BranchMigrationContextImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.buffer.Buffer;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeWebRootConflictEndpointTest extends AbstractMeshTest {

	/**
	 * Create two published nodes and move the second node into the folder of the first node. A conflict should occur since the node published segment path
	 * conflict with each other.
	 */
	@Test
	public void testDuplicateDueMove() {

		String conflictingName = "conflictName";
		try (Tx trx = tx()) {
			HibNode folderA = folder("2014");
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
			HibNode folderB = folder("2015");
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
		HibNode parent = tx(() -> folder("2015"));
		HibSchema contentSchema = tx(() -> schemaContainer("content"));

		tx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", createStringField("some title"));
			create.getFields().put("teaser", createStringField("some name"));
			create.getFields().put("slug", createStringField(conflictingName));
			create.getFields().put("content", createStringField("Blessed mealtime!"));
			call(() -> client().createNode(PROJECT_NAME, create));

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
	public void testCreateCaseSensitivityOfWebrootPath() {
		String conflictingName = "filename.html";
		HibNode parent = tx(() -> folder("2015"));
		HibSchema contentSchema = tx(() -> schemaContainer("content"));

		tx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(parent.getUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create.getFields().put("title", createStringField("some title"));
			create.getFields().put("teaser", createStringField("some name"));
			create.getFields().put("slug", createStringField(conflictingName));
			create.getFields().put("content", createStringField("Blessed mealtime!"));
			NodeResponse response1 = call(() -> client().createNode(PROJECT_NAME, create));

			// create the new content with same slug but different casing
			NodeCreateRequest create2 = new NodeCreateRequest();
			create2.setParentNodeUuid(parent.getUuid());
			create2.setLanguage("en");
			create2.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()).setUuid(contentSchema.getUuid()));
			create2.getFields().put("title", createStringField("some other title"));
			create2.getFields().put("teaser", createStringField("some other name"));
			create2.getFields().put("slug", createStringField(conflictingName.toUpperCase()));
			create2.getFields().put("content", createStringField("Blessed mealtime again!"));
			NodeResponse response2 = call(() -> client().createNode(PROJECT_NAME, create2));

			// check that both nodes can be retrieved via webroot path
			MeshWebrootResponse webrootResponse1 = call(() -> client().webroot(PROJECT_NAME, "/News/2015/" + conflictingName));
			Assertions.assertThat(webrootResponse1.getNodeResponse()).isEqualTo(response1);

			MeshWebrootResponse webrootResponse2 = call(() -> client().webroot(PROJECT_NAME, "/News/2015/" + conflictingName.toUpperCase()));
			Assertions.assertThat(webrootResponse2.getNodeResponse()).isEqualTo(response2);
		});
	}

	@Test
	public void testUpdateDuplicateWebrootPath() {
		String conflictingName = "filename.html";
		String nonConflictingName = "otherfilename.html";
		HibNode parent = tx(() -> folder("2015"));
		HibSchema contentSchema = tx(() -> schemaContainer("content"));

		tx(() -> {
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
		HibNode parent = tx(() -> folder("2015"));
		HibSchema contentSchema = tx(() -> schemaContainer("content"));

		tx(() -> {
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
		String parentUuid = tx(() -> folder("2015").getUuid());
		String otherParentUuid = tx(() -> folder("news").getUuid());

		// create the initial content
		NodeCreateRequest create = new NodeCreateRequest();
		create.setParentNodeUuid(parentUuid);
		create.setLanguage("en");
		create.setSchemaName("content");
		create.getFields().put("title", FieldUtil.createStringField("some title"));
		create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		String uuid = call(() -> client().createNode(PROJECT_NAME, create)).getUuid();

		// create a "conflicting" content in another folder
		NodeCreateRequest create2 = new NodeCreateRequest();
		create2.setParentNodeUuid(otherParentUuid);
		create2.setLanguage("en");
		create2.setSchemaName("content");
		create2.getFields().put("title", FieldUtil.createStringField("some other title"));
		create2.getFields().put("teaser", FieldUtil.createStringField("some other teaser"));
		create2.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		call(() -> client().createNode(PROJECT_NAME, create2));

		// try to move the original node
		call(() -> client().moveNode(PROJECT_NAME, uuid, otherParentUuid), CONFLICT, "node_conflicting_segmentfield_move", "slug",
			conflictingName);

	}

	@Test
	public void testDuplicateCrossBranches() {

		String conflictingName = "filename.html";
		String newBranchName = "newbranch";
		HibBranch initialBranch = tx(() -> initialBranch());
		String folderUuid = folderUuid();

		// 1. Create new branch and migrate nodes
		HibBranch newBranch = tx(() -> createBranch(newBranchName));
		BranchMigrationContextImpl context = new BranchMigrationContextImpl();
		context.setNewBranch(newBranch);
		context.setOldBranch(initialBranch);
		meshDagger().branchMigrationHandler().migrateBranch(context).blockingAwait();

		// 2. Create content in new branch
		NodeCreateRequest create = new NodeCreateRequest();
		create.setParentNodeUuid(folderUuid);
		create.setLanguage("en");
		create.setSchemaName("content");
		create.getFields().put("title", FieldUtil.createStringField("some title"));
		create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		call(() -> client().createNode(PROJECT_NAME, create));

		// 3. Create "conflicting" content in initial branch
		NodeCreateRequest create2 = new NodeCreateRequest();
		create2.setParentNodeUuid(folderUuid);
		create2.setLanguage("en");
		create2.setSchemaName("content");
		create2.getFields().put("title", FieldUtil.createStringField("some title"));
		create2.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create2.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		call(() -> client().createNode(PROJECT_NAME, create2, new VersioningParametersImpl().setBranch(initialBranchUuid())));
	}

	@Test
	public void testDuplicateCrossBranchesSameNode() {
		String conflictingName = "filename.html";
		String newBranchName = "newbranch";
		HibBranch initialBranch = tx(() -> initialBranch());
		String initialBranchUuid = initialBranchUuid();
		String folderUuid = tx(() -> folder("2015").getUuid());

		// 1. Create new branch and migrate nodes
		HibBranch newBranch = tx(() -> createBranch(newBranchName));
		BranchMigrationContextImpl context = new BranchMigrationContextImpl();
		context.setNewBranch(newBranch);
		context.setOldBranch(initialBranch);
		meshDagger().branchMigrationHandler().migrateBranch(context).blockingAwait();

		// 2. Create content in new branch
		NodeCreateRequest create = new NodeCreateRequest();
		create.setParentNodeUuid(folderUuid);
		create.setLanguage("en");
		create.setSchemaName("content");
		create.getFields().put("title", FieldUtil.createStringField("some title"));
		create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, create));

		// 3. Create "conflicting" content in initial branch
		NodeCreateRequest create2 = new NodeCreateRequest();
		create2.setParentNodeUuid(folderUuid);
		create2.setLanguage("en");
		create2.setSchemaName("content");
		create2.getFields().put("title", FieldUtil.createStringField("some title"));
		create2.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create2.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		call(() -> client().createNode(response.getUuid(), PROJECT_NAME, create2,
			new VersioningParametersImpl().setBranch(initialBranchUuid)));
	}

	@Test
	public void testDuplicateCrossBranchesSameNode1() {
		String conflictingName = "filename.html";
		String newBranchName = "newbranch";
		String folderUuid = tx(() -> folder("2015").getUuid());
		HibBranch initialBranch = tx(() -> initialBranch());

		// 1. Create new branch and migrate nodes
		HibBranch newBranch = tx(() -> createBranch(newBranchName));
		BranchMigrationContextImpl context = new BranchMigrationContextImpl();
		context.setOldBranch(initialBranch);
		context.setNewBranch(newBranch);
		meshDagger().branchMigrationHandler().migrateBranch(context).blockingAwait();

		// 2. Create "conflicting" content in initial branch
		NodeCreateRequest create = new NodeCreateRequest();
		create.setParentNodeUuid(folderUuid);
		create.setLanguage("en");
		create.setSchemaName("content");
		create.getFields().put("title", FieldUtil.createStringField("some title"));
		create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));

		NodeResponse response = call(
			() -> client().createNode(PROJECT_NAME, create, new VersioningParametersImpl().setBranch(initialBranchUuid())));

		// 3. Create content in new branch
		NodeCreateRequest create2 = new NodeCreateRequest();
		create2.setParentNodeUuid(folder("2015").getUuid());
		create2.setLanguage("en");
		create2.setSchemaName("content");
		create2.getFields().put("title", FieldUtil.createStringField("some title"));
		create2.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create2.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		call(() -> client().createNode(response.getUuid(), PROJECT_NAME, create2));
	}

	@Test
	public void testDuplicateWithOldVersion() {
		String conflictingName = "filename.html";
		String newName = "changed.html";
		String folderUuid = tx(() -> folder("2015").getUuid());

		// 1. Create initial content
		NodeCreateRequest create = new NodeCreateRequest();
		create.setParentNodeUuid(folderUuid);
		create.setLanguage("en");
		create.setSchemaName("content");
		create.getFields().put("title", FieldUtil.createStringField("some title"));
		create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		String nodeUuid = call(() -> client().createNode(PROJECT_NAME, create)).getUuid();

		// 2. Modify initial content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.setVersion("0.1");
		update.getFields().put("slug", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));

		// 3. Create "conflicting" content
		NodeCreateRequest create2 = new NodeCreateRequest();
		create2.setParentNodeUuid(folderUuid);
		create2.setLanguage("en");
		create2.setSchemaName("content");
		create2.getFields().put("title", FieldUtil.createStringField("some title"));
		create2.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create2.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		call(() -> client().createNode(PROJECT_NAME, create2)).getUuid();
	}

	@Test
	public void testDuplicateWithDrafts() {
		String initialName = "filename.html";
		String conflictingName = "changed.html";
		String folderUuid = tx(() -> folder("2015").getUuid());

		// 1. Create and publish initial content
		NodeCreateRequest create = new NodeCreateRequest();
		create.setParentNodeUuid(folderUuid);
		create.setLanguage("en");
		create.setSchemaName("content");
		create.getFields().put("title", FieldUtil.createStringField("some title"));
		create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create.getFields().put("slug", FieldUtil.createStringField(initialName));
		create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		String nodeUuid = call(() -> client().createNode(PROJECT_NAME, create)).getUuid();

		// 2. Modify initial content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.setVersion("0.1");
		update.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));

		// 3. Create content. The filename should not cause a conflict since the other node was just updated.
		NodeCreateRequest create2 = new NodeCreateRequest();
		create2.setParentNodeUuid(folderUuid);
		create2.setLanguage("en");
		create2.setSchemaName("content");
		create2.getFields().put("title", FieldUtil.createStringField("some title"));
		create2.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create2.getFields().put("slug", FieldUtil.createStringField(initialName));
		create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		String otherNodeUuid = call(() -> client().createNode(PROJECT_NAME, create2)).getUuid();

		// 4. Modify the second node in order to cause a conflict
		NodeUpdateRequest update2 = new NodeUpdateRequest();
		update2.setLanguage("en");
		update2.setVersion("0.1");
		update2.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		call(() -> client().updateNode(PROJECT_NAME, otherNodeUuid, update2), CONFLICT, "node_conflicting_segmentfield_update", "slug",
			conflictingName);

	}

	@Test
	public void testDuplicateWithPublished() {
		String conflictingName = "filename.html";
		String newName = "changed.html";
		String folderUuid = tx(() -> folder("2015").getUuid());

		// 1. Create and publish initial content
		NodeCreateRequest create = new NodeCreateRequest();
		create.setParentNodeUuid(folderUuid);
		create.setLanguage("en");
		create.setSchemaName("content");
		create.getFields().put("title", FieldUtil.createStringField("some title"));
		create.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		String createdUuid = call(() -> client().createNode(PROJECT_NAME, create)).getUuid();

		call(() -> client().publishNode(PROJECT_NAME, createdUuid));

		// 2. Modify initial content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.setVersion("0.1");
		update.getFields().put("slug", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, createdUuid, update));

		// 3. Create conflicting content
		NodeCreateRequest create2 = new NodeCreateRequest();
		create2.setParentNodeUuid(folderUuid);
		create2.setLanguage("en");
		create2.setSchemaName("content");
		create2.getFields().put("title", FieldUtil.createStringField("some title"));
		create2.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		create2.getFields().put("slug", FieldUtil.createStringField(conflictingName));
		create2.getFields().put("content", FieldUtil.createStringField("Blessed mealtime!"));
		String otherNodeUuid = call(() -> client().createNode(PROJECT_NAME, create2)).getUuid();

		// 4. Publish conflicting content
		call(() -> client().publishNode(PROJECT_NAME, otherNodeUuid), CONFLICT, "node_conflicting_segmentfield_publish", "slug",
			conflictingName);
	}

	@Test
	public void testBinaryNonSegmentFieldBreaksUniquenessNodeApi() {
		SchemaResponse binContentSchema = tx(tx -> {
			SchemaCreateRequest request = new SchemaCreateRequest();
			request.setName("schema_" + System.currentTimeMillis());
			request.setContainer(false);
			request.setSegmentField("name");

			StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
			nameFieldSchema.setName("name");
			nameFieldSchema.setLabel("Name");
			request.addField(nameFieldSchema);

			BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
			binaryFieldSchema.setName("binary");
			binaryFieldSchema.setLabel("Binary Data");
			request.addField(binaryFieldSchema);

			SchemaResponse schema = call(() -> client().createSchema(request));

			call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

			return schema;
		});		

		tx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folderUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(binContentSchema.getName()).setUuid(binContentSchema.getUuid()));
			create.getFields().put("name", FieldUtil.createStringField("title_12345"));
			NodeResponse node1 = client().createNode(project().getName(), create).toSingle().blockingGet();

			// create the illegal content - node cannot appear with already used segment value
			NodeCreateRequest illegalCreate = new NodeCreateRequest();
			illegalCreate.setParentNodeUuid(folderUuid());
			illegalCreate.setLanguage("en");
			illegalCreate.setSchema(new SchemaReferenceImpl().setName(binContentSchema.getName()).setUuid(binContentSchema.getUuid()));
			illegalCreate.getFields().put("name", FieldUtil.createStringField("title_12345"));
			call(() -> client().createNode(PROJECT_NAME, illegalCreate), CONFLICT, "node_conflicting_segmentfield_update", "name", "title_12345");

			// update binary field
			Buffer buffer = getBuffer("/pictures/android-gps.jpg");
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, node1.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "test.jpg", "image/jpeg"));

			// check the node still cannot appear with the same segment value
			call(() -> client().createNode(PROJECT_NAME, illegalCreate), CONFLICT, "node_conflicting_segmentfield_update", "name", "title_12345");

			// cleanup
			call(() -> client().deleteNode(PROJECT_NAME, node1.getUuid()));
			call(() -> client().unassignSchemaFromProject(PROJECT_NAME, binContentSchema.getUuid()));
			call(() -> client().deleteSchema(binContentSchema.getUuid()));
		});
	}


	@Test
	public void testBinaryNonSegmentFieldBreaksUniquenessWebrootApi() {
		SchemaResponse binContentSchema = tx(tx -> {
			SchemaCreateRequest request = new SchemaCreateRequest();
			request.setName("schema_" + System.currentTimeMillis());
			request.setContainer(false);
			request.setSegmentField("name");

			StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
			nameFieldSchema.setName("name");
			nameFieldSchema.setLabel("Name");
			request.addField(nameFieldSchema);

			BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
			binaryFieldSchema.setName("binary");
			binaryFieldSchema.setLabel("Binary Data");
			request.addField(binaryFieldSchema);

			SchemaResponse schema = call(() -> client().createSchema(request));

			call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

			return schema;
		});		

		tx(() -> {
			// create the initial content
			NodeCreateRequest create = new NodeCreateRequest();
			create.setParentNodeUuid(folderUuid());
			create.setLanguage("en");
			create.setSchema(new SchemaReferenceImpl().setName(binContentSchema.getName()).setUuid(binContentSchema.getUuid()));
			create.getFields().put("name", FieldUtil.createStringField("title_12345"));
			NodeResponse node1 = client().webrootCreate(PROJECT_NAME, "/title_12345", create).toSingle().blockingGet();

			// create the illegal content - node cannot appear with already used segment value
			NodeCreateRequest illegalCreate = new NodeCreateRequest();
			illegalCreate.setParentNodeUuid(folderUuid());
			illegalCreate.setLanguage("en");
			illegalCreate.setSchema(new SchemaReferenceImpl().setName(binContentSchema.getName()).setUuid(binContentSchema.getUuid()));
			illegalCreate.getFields().put("name", FieldUtil.createStringField("title_12345"));
			call(() -> client().webrootCreate(PROJECT_NAME, "/title_12345", illegalCreate), CONFLICT, "node_conflicting_segmentfield_update", "name", "title_12345");

			// update binary field
			Buffer buffer = getBuffer("/pictures/android-gps.jpg");
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, node1.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "test.jpg", "image/jpeg"));

			// check the node still cannot appear with the same segment value
			call(() -> client().webrootCreate(PROJECT_NAME, "/title_12345", illegalCreate), CONFLICT, "node_conflicting_segmentfield_update", "name", "title_12345");

			// cleanup
			call(() -> client().deleteNode(PROJECT_NAME, node1.getUuid()));
			call(() -> client().unassignSchemaFromProject(PROJECT_NAME, binContentSchema.getUuid()));
			call(() -> client().deleteSchema(binContentSchema.getUuid()));
		});
	}
}
