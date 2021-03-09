package com.gentics.mesh.distributed.coordinatortoken;

import org.junit.Ignore;

import com.gentics.mesh.rest.client.MeshRestClient;

/**
 * Executes the tests from {@link AbstractClusterCoordinatorTokenTest}.
 * This is intended for local testing to provide a quicker test run execution.
 *
 * This requires the following setup:
 * <ul>
 *     <li>A Mesh cluster with two nodes "nodeA" and "nodeB"</li>
 *     <li>The cluster coordination has to activated and nodeA must be the master coordinator.</li>
 *     <li>The AuthPlugin (found in <code>core/src/test/plugins</code>) must be deployed on both systems.</li>
 *     <li>Both nodes must use the JWK found in the resources (<code>/public-keys/symmetric-key.json</code>).</li>
 *     <li>nodeB must listen to port 8081</li>
 *     <li>Both nodes must use the same keystore.</li>
 * </ul>
 *
 */
public class ClusterCoordinatorTokenTestLocal extends AbstractClusterCoordinatorTokenTest {

	@Override
	protected MeshRestClient getServerBClient() {
		return MeshRestClient.create("localhost", 8081, false);
	}
}
