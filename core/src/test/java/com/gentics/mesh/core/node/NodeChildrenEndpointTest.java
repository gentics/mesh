package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.TestUtils.size;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeChildrenEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadChildrenOfBaseNode() {
		try (Tx tx = tx()) {
			call(() -> client().findNodeChildren(PROJECT_NAME, project().getBaseNode().getUuid()));
		}
	}

	@Test
	public void testNodeHierarchy() {
		try (Tx tx = tx()) {
			Node baseNode = project().getBaseNode();
			String parentNodeUuid = baseNode.getUuid();
			NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().draft()));
			assertEquals(3, nodeList.getData().size());

			NodeCreateRequest create1 = new NodeCreateRequest();
			SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
			schemaReference.setName("folder");
			create1.setSchema(schemaReference);
			create1.setLanguage("en");
			create1.setParentNodeUuid(parentNodeUuid);
			NodeResponse createdNode = call(() -> client().createNode(PROJECT_NAME, create1));

			String uuid = createdNode.getUuid();
			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
			assertEquals(0, nodeList.getData().size());

			NodeCreateRequest create2 = new NodeCreateRequest();
			create2.setSchema(schemaReference);
			create2.setLanguage("en");
			create2.setParentNodeUuid(uuid);
			createdNode = call(() -> client().createNode(PROJECT_NAME, create2));

			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
			assertEquals("The subnode did not contain the created node", 1, nodeList.getData().size());

			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().draft()));
			assertEquals("The basenode should still contain four nodes.", 4, nodeList.getData().size());
		}
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("news");
			assertNotNull(node);
			assertNotNull(node.getUuid());
			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertThat(node).matches(restNode);
			assertTrue(restNode.getContainer());

			long subFolderCount = restNode.getChildrenInfo().get("folder").getCount();
			assertEquals("The node should have more than {" + subFolderCount + "} children. But it got {" + subFolderCount + "}", 2, subFolderCount);

			long subContentCount = restNode.getChildrenInfo().get("content").getCount();
			assertEquals("The node should have more than {" + subContentCount + "} children. But it got {" + subContentCount + "}", 1,
					subContentCount);
		}
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildrenPermissions() throws Exception {
		Node node = folder("news");
		try (Tx tx = tx()) {
			RoleRoot roleDao = tx.data().roleDao();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			roleDao.revokePermissions(role(), folder("2015"), READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertThat(node).matches(restNode);
			assertTrue(restNode.getContainer());

			long subFolderCount = restNode.getChildrenInfo().get("folder").getCount();
			assertEquals("The node should have more than {" + subFolderCount + "} children. But it got {" + subFolderCount + "}", 1, subFolderCount);

			long subContentCount = restNode.getChildrenInfo().get("content").getCount();
			assertEquals("The node should have more than {" + subContentCount + "} children. But it got {" + subContentCount + "}", 1,
					subContentCount);
		}
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren2() throws Exception {
		try (Tx tx = tx()) {
			Node node = content("concorde");
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertThat(node).matches(restNode);
			assertFalse("The node should not be a container", restNode.getContainer());
			assertNull(restNode.getChildrenInfo().get("folder"));
		}
	}

	@Test
	public void testReadNodeChildren() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("news");
			assertNotNull(node);
			assertNotNull(node.getUuid());

			long size = size(node.getChildren());
			long expectedItemsInPage =  size > 25 ? 25 : size;

			NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(),
					new VersioningParametersImpl().draft()));

			assertEquals(size(node.getChildren()), nodeList.getMetainfo().getTotalCount());
			assertEquals(expectedItemsInPage, nodeList.getData().size());
		}
	}

	@Test
	public void testReadNodeChildrenWithoutChildPermission() throws Exception {
		Node node = folder("news");
		Node nodeWithNoPerm = folder("2015");
		try (Tx tx = tx()) {
			RoleRoot roleDao = tx.data().roleDao();
			assertNotNull(node);
			assertNotNull(node.getUuid());
			roleDao.revokePermissions(role(), nodeWithNoPerm, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(),
					new PagingParametersImpl().setPerPage(20000L), new VersioningParametersImpl().draft()));

			assertEquals(0, nodeList.getData().stream().filter(p -> nodeWithNoPerm.getUuid().equals(p.getUuid())).count());
			assertEquals(2, nodeList.getData().size());
			assertEquals(size(node.getChildren()) - 1, nodeList.getMetainfo().getTotalCount());
		}
	}

	@Test
	public void testReadNodeChildrenWithNoPermission() throws Exception {
		Node node = folder("news");
		try (Tx tx = tx()) {
			RoleRoot roleDao = tx.data().roleDao();
			assertNotNull(node);
			assertNotNull(node.getUuid());
			roleDao.revokePermissions(role(), node, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(), new NodeParametersImpl()), FORBIDDEN,
					"error_missing_perm", node.getUuid(), READ_PERM.getRestPerm().getName());
		}

	}

	@Test
	public void testReadBranchChildren() {
		Node node = folder("news");
		long childrenSize;
		long expectedItemsInPage;
		Branch newBranch;
		Node firstFolder;

		try (Tx tx = tx()) {
			firstFolder = node.getChildren()
				.stream()
				.filter(child -> child.getSchemaContainer().getName().equals("folder"))
				.findAny().get();
			childrenSize = size(node.getChildren());
			expectedItemsInPage = childrenSize > 25 ? 25 : childrenSize;
			newBranch = createBranch("newbranch");
			tx.success();
		}

		// "migrate" the News node to the new branch, so that we can get children of it in both branches
		try (Tx tx = tx()) {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setLanguage("en");
			create.getFields().put("name", FieldUtil.createStringField("News"));
			create.setParentNodeUuid(node.getParentNode(initialBranch().getUuid()).getUuid());
			call(() -> client().createNode(node.getUuid(), PROJECT_NAME, create));

			tx.success();
		}

		try (Tx tx = tx()) {
			NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(),
					new VersioningParametersImpl().setBranch(initialBranch().getName()).draft()));
			assertEquals("Total children in initial branch", childrenSize, nodeList.getMetainfo().getTotalCount());
			assertEquals("Returned children in initial branch", expectedItemsInPage, nodeList.getData().size());

			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(),
					new VersioningParametersImpl().setBranch(newBranch.getName()).draft()));
			assertEquals("Total children in new branch", 0, nodeList.getMetainfo().getTotalCount());
			assertEquals("Returned children in new branch", 0, nodeList.getData().size());

			NodeCreateRequest create = new NodeCreateRequest();
			create.setLanguage("en");
			create.getFields().put("name", FieldUtil.createStringField("new"));
			create.setParentNodeUuid(node.getUuid());
			call(() -> client().createNode(firstFolder.getUuid(), PROJECT_NAME, create));

			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(),
					new VersioningParametersImpl().setBranch(newBranch.getName()).draft()));
			assertEquals("Total children in new branch", 1, nodeList.getMetainfo().getTotalCount());
			assertEquals("Returned children in new branch", 1, nodeList.getData().size());
		}
	}

	@Test
	public void testFilterByLanguage() {
		String uuid = db().tx(() -> folder("2015").getUuid());

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("de");
		nodeCreateRequest.setParentNode(new NodeReference().setUuid(uuid));
		nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("content"));
		nodeCreateRequest.getFields().put("teaser", new StringFieldImpl().setString("Only German Teaser"));
		nodeCreateRequest.getFields().put("slug", new StringFieldImpl().setString("Only German Slug"));

		call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		NodeListResponse listResponse = call(() -> client().findNodeChildren(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("en")));
		List<String> langList = listResponse.getData().stream().map(node -> node.getLanguage()).collect(Collectors.toList());
		assertThat(langList).doesNotContain(null, "de");
	}

	@Test
	public void testReadPublishedChildren() {
		// TODO
	}

	@Test
	public void testReadBranchPublishedChildren() {
		// TODO
	}
}
