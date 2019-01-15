package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.callETag;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeNavigationEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().loadNavigation(PROJECT_NAME, project().getBaseNode().getUuid()));
			callETag(() -> client().loadNavigation(PROJECT_NAME, project().getBaseNode().getUuid()), etag, true, 304);
		}
	}

}
