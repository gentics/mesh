package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;

public class NodeVerticleETagTest extends AbstractETagTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	@Override
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<NodeListResponse> response = getClient().findNodes(PROJECT_NAME).invoke();
			latchFor(response);
			String etag = response.getResponse().getHeader(ETAG);
			assertNotNull(etag);

			expect304(getClient().findNodes(PROJECT_NAME), etag);
			expectNo304(getClient().findNodes(PROJECT_NAME, new PagingParameters().setPage(2)), etag);
		}
	}

	@Test
	@Override
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			MeshResponse<NodeResponse> response = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid()).invoke();
			latchFor(response);
			String etag = node.getETag(getMockedInternalActionContext());
			assertEquals(etag, response.getResponse().getHeader(ETAG));

			// Check whether 304 is returned for correct etag
			MeshRequest<NodeResponse> request = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid());
			assertEquals(etag, expect304(request, etag));

			assertNotEquals("A different etag should have been generated since we are not requesting the expanded node.", etag,
					expectNo304(getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParameters().setExpandAll(true)), etag));

			// Assert that adding bogus query parameters will not affect the etag
			//			expect304(getClient().findNodeByUuid(node.getUuid(), new NodeParameters().setExpandAll(false)), etag);
			//			expect304(getClient().findNodeByUuid(node.getUuid(), new NodeParameters().setExpandAll(true)), etag);
		}

	}

}
