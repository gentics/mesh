package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.util.MeshAssert.latchFor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

import io.vertx.core.AbstractVerticle;

public class NodeNavigationVerticleETagTest extends AbstractETagTest {

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<NavigationResponse> response = getClient().loadNavigation(PROJECT_NAME, project().getBaseNode().getUuid()).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			expect304(getClient().loadNavigation(PROJECT_NAME, project().getBaseNode().getUuid()), etag, true);
		}
	}

}
