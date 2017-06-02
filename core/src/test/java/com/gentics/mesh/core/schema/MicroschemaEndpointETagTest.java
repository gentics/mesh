package com.gentics.mesh.core.schema;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.callETag;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class MicroschemaEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().findMicroschemas());
			callETag(() -> client().findMicroschemas(), etag, true, 304);
			callETag(() -> client().findMicroschemas(new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			MicroschemaContainer schema = microschemaContainers().get("vcard");

			String actualEtag = callETag(() -> client().findMicroschemaByUuid(schema.getUuid()));
			String etag = schema.getETag(mockActionContext());
			assertEquals(etag, actualEtag);

			// Check whether 304 is returned for correct etag
			MeshRequest<MicroschemaResponse> request = client().findMicroschemaByUuid(schema.getUuid());
			assertThat(callETag(() -> request, etag, true, 304)).contains(etag);

			// The node has no node reference and thus expanding will not affect the etag
			assertThat(callETag(() -> client().findMicroschemaByUuid(schema.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304))
					.contains(etag);

			// Assert that adding bogus query parameters will not affect the etag
			callETag(() -> client().findMicroschemaByUuid(schema.getUuid(), new NodeParametersImpl().setExpandAll(false)), etag, true, 304);
			callETag(() -> client().findMicroschemaByUuid(schema.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304);
		}

	}

}
