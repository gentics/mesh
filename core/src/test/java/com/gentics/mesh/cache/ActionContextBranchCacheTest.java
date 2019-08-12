package com.gentics.mesh.cache;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class ActionContextBranchCacheTest extends AbstractMeshTest {

	@Before
	public void setupCache() {
		cache().enable();
	}

	private EventAwareCache<String, Branch> cache() {
		return mesh().branchCache().cache();
	}

	@Test
	public void testBranchCache() {
		String newName = "New Branch Name";

		assertFalse("Initially the cache should not contain the branch", hasBranchInCache());
		assertEquals(0, branchCacheSize());
		call(() -> client().findNodes(projectName(), new VersioningParametersImpl().setBranch(initialBranchUuid())));
		assertTrue("The branch should now be cached", hasBranchInCache());
		assertEquals(1, branchCacheSize());

		// Update the branch
		BranchUpdateRequest request1 = new BranchUpdateRequest().setName(newName);
		call(() -> client().updateBranch(projectName(), initialBranchUuid(), request1));

		assertFalse("The cache should have been invalidated.", hasBranchInCache());
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
		return cache().get(project().id() + "-" + initialBranchUuid()) != null;
	}
}
