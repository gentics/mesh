package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeDeleteBranchEndpointTest extends AbstractMeshTest {
	private final String newBranch = "newBranch";

	@Test
	public void deleteNodeInBranch() {
		grantAdmin();
		NodeResponse parent = createNode();
		NodeResponse otherParent = createNode();
		publishNode(parent);
		publishNode(otherParent);

		waitForJob(() -> createBranchRest(newBranch));

		NodeResponse child = createNode(parent);
		publishNode(child);

		NodeListResponse children = client().findNodeChildren(PROJECT_NAME, parent.getUuid(), new VersioningParametersImpl().setBranch(initialBranchUuid())).blockingGet();

		assertThat(children.getData()).isEmpty();

		createContentInBranchWithOtherParent(child, otherParent, initialBranchUuid());

		client().deleteNode(PROJECT_NAME, otherParent.getUuid(),
			new DeleteParametersImpl().setRecursive(true)
		).blockingAwait();

	}

	private void createContentInBranchWithOtherParent(NodeResponse node, NodeResponse parent, String branch) {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNodeUuid(parent.getUuid());
		request.getFields().put("slug", new StringFieldImpl().setString(RandomStringUtils.randomAlphabetic(5)));
		client().createNode(node.getUuid(), PROJECT_NAME, request, new VersioningParametersImpl().setBranch(branch)).blockingAwait();
		publishNodeInBranch(node, branch);
	}
}
