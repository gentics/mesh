package com.gentics.mesh.core.node;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeNavigationEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			MeshResponse<NavigationResponse> response = client().loadNavigation(PROJECT_NAME, project().getBaseNode().getUuid()).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getRawResponse().getHeader(ETAG));
			callETag(() -> client().loadNavigation(PROJECT_NAME, project().getBaseNode().getUuid()), etag, true, 304);
		}
	}

}
