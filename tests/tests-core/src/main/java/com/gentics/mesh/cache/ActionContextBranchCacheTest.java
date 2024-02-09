package com.gentics.mesh.cache;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.helper.ExpectedEvent;

@MeshTestSetting(testSize = FULL, startServer = true)
public class ActionContextBranchCacheTest extends AbstractMeshTest {

	@Before
	public void setupCache() {
		cache().enable();
	}

	private ProjectBranchNameCache cache() {
		return mesh().branchCache();
	}

	@Test
	public void testBranchCacheResetOnNewLatest() {
		BranchResponse branch = call(() -> client().createBranch(projectName(), new BranchCreateRequest().setName("new").setLatest(false)));
		assertNotNull(branch.getUuid());
		assertEquals(branch.getName(), "new");
		assertFalse(branch.getLatest());
		String branchUuid = branch.getUuid();

		NodeResponse node = call(() -> client().createNode(projectName(), new NodeCreateRequest().setSchemaName("folder").setLanguage(english()).setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()))));

		// The latest branch contains the new node
		NodeListResponse branchNodes = call(() -> client().findNodeChildren(projectName(), tx(() -> project().getBaseNode().getUuid())));
		assertThat(branchNodes.getData()).usingElementComparator((a, b) -> a.getUuid().compareTo(b.getUuid())).contains(node);

		// New latest branch
		branch = call(() -> client().setLatestBranch(projectName(), branchUuid));

		// It should have no old node
		branchNodes = call(() -> client().findNodeChildren(projectName(), tx(() -> project().getBaseNode().getUuid())));
		assertThat(branchNodes.getData()).usingElementComparator((a, b) -> a.getUuid().compareTo(b.getUuid())).doesNotContain(node);
	}

	@Test
	public void testBranchCache() throws TimeoutException {
		String newName = "New Branch Name";

		assertFalse("Initially the cache should not contain the branch", hasBranchInCache());
		assertEquals(0, branchCacheSize());
		call(() -> client().findNodes(projectName(), new VersioningParametersImpl().setBranch(initialBranchUuid())));
		assertTrue("The branch should now be cached", hasBranchInCache());
		assertEquals(1, branchCacheSize());

		// Update the branch
		BranchUpdateRequest request1 = new BranchUpdateRequest().setName(newName);
		try (ExpectedEvent ee = expectEvent(MeshEvent.BRANCH_UPDATED, 10_000)) {
			call(() -> client().updateBranch(projectName(), initialBranchUuid(), request1));
		}

		// Event is processed async and thus the cache clear is also done async
		assertThat(waitFor(() -> !hasBranchInCache(), 10_000)).as("Cache has been invalidated within given timeout").isTrue();
		assertEquals(0, branchCacheSize());

		// Disable the cache and check caching
		cache().disable();
		call(() -> client().findNodes(projectName(), new VersioningParametersImpl().setBranch(initialBranchUuid())));
		assertFalse("The cache should still not have the entry", hasBranchInCache());
		assertEquals(0, branchCacheSize());
	}

	private long branchCacheSize() {
		return cache().size();
	}

	public boolean hasBranchInCache() {
		return cache().get(project().getId() + "-" + initialBranchUuid()) != null;
	}
}
