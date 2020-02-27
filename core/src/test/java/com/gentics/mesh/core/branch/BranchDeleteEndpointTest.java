package com.gentics.mesh.core.branch;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class BranchDeleteEndpointTest extends AbstractMeshTest {

	/**
	 * Deletes a branch where there is a node with contents only in the deleted branch.
	 */
	@Test
	public void testNodeOnlyInDeletedBranch() {
		// TODO implement
		throw new RuntimeException("Not implemented");
	}

	/**
	 * Deletes a branch where there is a node which has a language only in the deleted branch, but also has languages
	 * in other branches.
	 */
	@Test
	public void testLanguageOnlyInDeletedBranch() {
		// TODO implement
		throw new RuntimeException("Not implemented");
	}

	/**
	 * Deletes a branch where there is a node with contents in multiple branches.
	 */
	@Test
	public void testWithOtherBranch() {

	}
}
