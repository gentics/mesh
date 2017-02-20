package com.gentics.mesh.test;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.http.HttpConstants.IF_NONE_MATCH;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.ETag;

public abstract class AbstractETagTest extends AbstractMeshTest {

	/**
	 * Set the if-none-match header with the given etag and assert that the response contains the full response instead of an 304 response with no body.
	 * 
	 * @param request
	 * @param etag
	 * @param isWeak
	 * @return etag from header response value
	 */
	protected String expectNo304(MeshRequest<?> request, String etag, boolean isWeak) {
		request.getRequest().putHeader(IF_NONE_MATCH, ETag.prepareHeader(etag, isWeak));
		MeshResponse<?> response = request.invoke();
		latchFor(response);
		assertNotNull("The response should not be null.", response.result());
		assertEquals("The response code was not 200.", 200, response.getResponse().statusCode());
		return ETag.extract(response.getResponse().getHeader(ETAG));
	}

	/**
	 * Set the if-none-match header using the given etag and assert that the response is an 304 response.
	 * 
	 * @param request
	 * @param etag
	 * @param isWeak
	 * @return
	 */
	protected String expect304(MeshRequest<?> request, String etag, boolean isWeak) {
		request.getRequest().putHeader(IF_NONE_MATCH, ETag.prepareHeader(etag, isWeak));
		MeshResponse<?> response = request.invoke();
		latchFor(response);
		assertEquals("We expected a 304 response.", 304, response.getResponse().statusCode());
		assertNull("The response should be null since we got a 304", response.result());
		return ETag.extract(response.getResponse().getHeader(ETAG));
	}
}
