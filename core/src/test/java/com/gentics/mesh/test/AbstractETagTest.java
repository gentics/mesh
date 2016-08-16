package com.gentics.mesh.test;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.http.HttpConstants.IF_NONE_MATCH;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.definition.ETagTestcases;

public abstract class AbstractETagTest extends AbstractIsolatedRestVerticleTest implements ETagTestcases {

	/**
	 * Set the if-none-match header with the given etag and assert that the response contains the full response instead of an 304 response with no body.
	 * 
	 * @param request
	 * @param etag
	 * @return
	 */
	protected String expectNo304(MeshRequest<?> request, String etag) {
		request.getRequest().putHeader(IF_NONE_MATCH, etag);
		MeshResponse<?> response = request.invoke();
		latchFor(response);
		assertNotNull("The response should not be null.", response.result());
		assertEquals("The response code was not 200.", 200, response.getResponse().statusCode());
		return response.getResponse().getHeader(ETAG);
	}

	/**
	 * Set the if-none-match header using the given etag and assert that the response is an 304 response.
	 * 
	 * @param request
	 * @param etag
	 * @return
	 */
	protected String expect304(MeshRequest<?> request, String etag) {
		request.getRequest().putHeader(IF_NONE_MATCH, etag);
		MeshResponse<?> response = request.invoke();
		latchFor(response);
		assertNull("The response should be null since we got a 304", response.result());
		assertEquals(304, response.getResponse().statusCode());
		return response.getResponse().getHeader(ETAG);
	}
}
