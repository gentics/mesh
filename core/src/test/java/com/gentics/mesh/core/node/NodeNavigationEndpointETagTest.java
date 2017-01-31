package com.gentics.mesh.core.node;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.latchFor;

import org.junit.Test;

import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

public class NodeNavigationEndpointETagTest extends AbstractETagTest {

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<NavigationResponse> response = client().loadNavigation(PROJECT_NAME, project().getBaseNode().getUuid()).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			expect304(client().loadNavigation(PROJECT_NAME, project().getBaseNode().getUuid()), etag, true);
		}
	}

}
