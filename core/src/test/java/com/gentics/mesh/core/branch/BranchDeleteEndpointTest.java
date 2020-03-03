package com.gentics.mesh.core.branch;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.ClientHandler;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.netty.handler.codec.http.HttpResponseStatus;

@MeshTestSetting(testSize = FULL, startServer = true)
public class BranchDeleteEndpointTest extends AbstractMeshTest {

	private BranchResponse initialBranch;
	private int branchCount;
	private int contentCount;

	@Before
	public void setUp() throws Exception {
		grantAdminRole();
		initialBranch = client().findBranches(PROJECT_NAME).blockingGet().getData().get(0);
		branchCount = 0;
		contentCount = 0;
	}

	/**
	 * Deletes a branch where there is a node with contents only in the deleted branch.
	 */
	@Test
	public void testNodeOnlyInDeletedBranch() {
		BranchResponse branch = createBranchRest("testBranch", false);
		NodeResponse node = addNode(branch);
		deleteBranch(branch);
		call(findNode(node, branch), HttpResponseStatus.NOT_FOUND, "object_not_found_for_uuid", node.getUuid());
		call(findNode(node, initialBranch), HttpResponseStatus.NOT_FOUND, "object_not_found_for_uuid", node.getUuid());
	}

	/**
	 * Deletes a branch where there is a node which has a language only in the deleted branch, but also has languages
	 * in other branches.
	 */
	@Test
	public void testLanguageOnlyInDeletedBranch() {
		NodeResponse node = addNode(initialBranch);
		BranchResponse branch = createBranchRest("testBranch", false);
		addContentInNewBranch(node, branch, new ContentBranchParams().setLanguage("de"));
		deleteBranch(branch);
		call(findNode(node, branch), HttpResponseStatus.BAD_REQUEST, "branch_error_not_found", branch.getUuid());
		call(findNode(node, initialBranch));
	}

	/**
	 * Deletes a branch where there is a node with contents in multiple branches.
	 */
	@Test
	public void testWithOtherBranch() {
		BranchResponse branch = createBranchRest("testBranch", false);
		NodeResponse node = addNode(initialBranch);
		addContentInNewBranch(node, branch);
		deleteBranch(branch);
		call(findNode(node, branch), HttpResponseStatus.BAD_REQUEST, "branch_error_not_found", branch.getUuid());
		call(findNode(node, initialBranch));
	}

	@Test
	public void testNodeWithParentsInDifferentBranches() {
		BranchResponse branch1 = createBranchRest("testBranch1", false);
		BranchResponse branch2 = createBranchRest("testBranch2", false);

		NodeResponse parent1 = addNode(branch1);
		NodeResponse parent2 = addNode(branch2);
		NodeResponse child = addNode(parent1, branch1);
		addContentInNewBranch(child, branch2, new ContentBranchParams().setParentNodeUuid(parent2));

		deleteBranch(branch1);
		call(findNode(parent1, branch1), HttpResponseStatus.NOT_FOUND, "object_not_found_for_uuid", parent1.getUuid());
		call(findNode(child, branch1), HttpResponseStatus.BAD_REQUEST, "branch_error_not_found", branch1.getUuid());
		call(findNode(parent2, branch2));
		call(findNode(child, branch2));

		deleteBranch(branch2);
		call(findNode(parent2, branch2), HttpResponseStatus.NOT_FOUND, "object_not_found_for_uuid", parent2.getUuid());
		call(findNode(child, branch1), HttpResponseStatus.NOT_FOUND, "object_not_found_for_uuid", child.getUuid());
	}

	@Test
	public void testBranchBetweenVersions() {
		NodeResponse node = addNode(initialBranch);
		addContent(node, initialBranch);
		addContent(node, initialBranch);

		BranchResponse branch = createBranchRest("testBranch", false);

		call(findNode(node, initialBranch));
		call(findNode(node, branch));

		addContent(node, initialBranch);
		addContent(node, initialBranch);

		deleteBranch(branch);
		call(findNode(node, initialBranch));
		call(findNode(node, branch), HttpResponseStatus.BAD_REQUEST, "branch_error_not_found", branch.getUuid());
	}

	@Test
	public void testBranchBetweenPublishedVersions() {
		NodeResponse node = addNode(initialBranch);
		addContent(node, initialBranch);
		addContent(node, initialBranch);
		publishNode(node, initialBranch);

		BranchResponse branch = createBranchRest("testBranch", false);

		call(findNode(node, initialBranch));
		call(findNode(node, branch));

		addContent(node, initialBranch);
		addContent(node, initialBranch);
		publishNode(node, initialBranch);

		deleteBranch(branch);
		call(findNode(node, initialBranch));
		call(findNode(node, branch), HttpResponseStatus.BAD_REQUEST, "branch_error_not_found", branch.getUuid());
	}


	@Test
	public void testBranchBetweenVersionsWithAdditionalContent() {
		NodeResponse node = addNode(initialBranch);
		addContent(node, initialBranch);
		addContent(node, initialBranch);
		publishNode(node, initialBranch);

		BranchResponse branch = createBranchRest("testBranch", false);

		call(findNode(node, initialBranch));
		call(findNode(node, branch));

		addContent(node, initialBranch);
		addContent(node, initialBranch);
		addContent(node, branch);
		addContent(node, branch);

		deleteBranch(branch);
		call(findNode(node, initialBranch));
		call(findNode(node, branch), HttpResponseStatus.BAD_REQUEST, "branch_error_not_found", branch.getUuid());
	}

	@Test
	public void testBranchBetweenPublishedVersionsWithAdditionalContent() {
		NodeResponse node = addNode(initialBranch);
		addContent(node, initialBranch);
		addContent(node, initialBranch);
		publishNode(node, initialBranch);

		BranchResponse branch = createBranchRest("testBranch", false);

		call(findNode(node, initialBranch));
		call(findNode(node, branch));

		addContent(node, initialBranch);
		addContent(node, initialBranch);
		publishNode(node, initialBranch);
		addContent(node, branch);
		addContent(node, branch);
		publishNode(node, branch);

		deleteBranch(branch);
		call(findNode(node, initialBranch));
		call(findNode(node, branch), HttpResponseStatus.BAD_REQUEST, "branch_error_not_found", branch.getUuid());
	}

	/**
	 * Tries to delete the latest branch.
	 */
	@Test
	public void testDeleteLatestBranch() {
		String branchUuid = initialBranchUuid();
		call(
			() -> client().deleteBranch(PROJECT_NAME, branchUuid),
			HttpResponseStatus.BAD_REQUEST,
			"branch_error_delete_latest", branchUuid
		);
	}

	@Test
	public void testDeleteInitialBranch() {
		NodeResponse node = addNode(initialBranch);
		addContent(node, initialBranch);
		publishNode(node, initialBranch);

		BranchResponse testBranch = createBranchRest("testBranch", true);

		deleteBranch(initialBranch);

		call(findNode(node, initialBranch), HttpResponseStatus.BAD_REQUEST, "branch_error_not_found", initialBranch.getUuid());
		call(findNode(node, testBranch));
	}

	@Test
	public void testDeleteInitialBranchWithAdditionalContent() {
		NodeResponse node = addNode(initialBranch);
		addContent(node, initialBranch);
		publishNode(node, initialBranch);

		BranchResponse testBranch = createBranchRest("testBranch", true);

		addContent(node, initialBranch);
		publishNode(node, initialBranch);

		deleteBranch(initialBranch);

		call(findNode(node, initialBranch), HttpResponseStatus.BAD_REQUEST, "branch_error_not_found", initialBranch.getUuid());
		call(findNode(node, testBranch));
	}

	private BranchResponse addBranch() {
		return createBranchRest("testBranch" + branchCount++, false);
	}

	private BranchResponse addBranch(BranchResponse previousBranch) {
		BranchCreateRequest branchCreateRequest = new BranchCreateRequest()
			.setName("testBranch" + branchCount++)
			.setLatest(false)
			.setBaseBranch(previousBranch.toReference());
		return createBranchRest("testBranch" + branchCount++, false);
	}

	private NodeResponse addNode(NodeResponse parentNode, BranchResponse branch) {
		return addNode(parentNode.getUuid(), branch);
	}

	private NodeResponse addNode(BranchResponse branch) {
		return addNode(folderUuid(), branch);
	}

	private NodeResponse addNode(String parentNodeUuid, BranchResponse branch) {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setParentNodeUuid(parentNodeUuid);
		request.setLanguage("de");
		request.setSchemaName("folder");
		return client().createNode(
			PROJECT_NAME, request,
			new VersioningParametersImpl().setBranch(branch.getName())
		).blockingGet();
	}

	private ClientHandler<NodeResponse> findNode(NodeResponse node, BranchResponse branch) {
		return () -> client().findNodeByUuid(
			PROJECT_NAME,
			node.getUuid(),
			new VersioningParametersImpl().setBranch(branch.getUuid())
		);
	}

	private NodeResponse addContent(NodeResponse node, BranchResponse branch) {
		return addContent(node, branch, "en");
	}

	private NodeResponse addContent(NodeResponse node, BranchResponse branch, String language) {
		NodeUpdateRequest request = new NodeUpdateRequest()
			.setLanguage(language)
			.setVersion("draft")
			.setFields(FieldMap.of(
				"name", StringField.of("name" + contentCount++)
			));

		return client().updateNode(
			PROJECT_NAME,
			node.getUuid(),
			request,
			new VersioningParametersImpl().setBranch(branch.getUuid())
		).blockingGet();
	}

	private NodeResponse addContentInNewBranch(NodeResponse node, BranchResponse branch) {
		return addContentInNewBranch(node, branch, new ContentBranchParams());
	}

	private NodeResponse addContentInNewBranch(NodeResponse node, BranchResponse branch, ContentBranchParams contentBranchParams) {
		NodeCreateRequest request = new NodeCreateRequest()
			.setLanguage(contentBranchParams.getLanguage())
			.setParentNodeUuid(contentBranchParams.getParentNodeUuid())
			.setFields(FieldMap.of(
				"name", StringField.of("name" + contentCount++)
			));

		if (contentBranchParams.getParentNodeUuid() == null) {
			request.setParentNodeUuid(node.getParentNode().getUuid());
		}

		return client().createNode(
			node.getUuid(),
			PROJECT_NAME,
			request,
			new VersioningParametersImpl().setBranch(branch.getUuid())
		).blockingGet();
	}

	private static class ContentBranchParams {
		private String parentNodeUuid;
		private String language = "en";

		public String getParentNodeUuid() {
			return parentNodeUuid;
		}

		public ContentBranchParams setParentNodeUuid(NodeResponse parentNode) {
			return setParentNodeUuid(parentNode.getUuid());
		}

		public ContentBranchParams setParentNodeUuid(String parentNodeUuid) {
			this.parentNodeUuid = parentNodeUuid;
			return this;
		}

		public String getLanguage() {
			return language;
		}

		public ContentBranchParams setLanguage(String language) {
			this.language = language;
			return this;
		}
	}
}
