package com.gentics.mesh.core.schema;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

public class MicroschemaVerticleETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<MicroschemaListResponse> response = getClient().findMicroschemas().invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(getClient().findMicroschemas(), etag, true);
			expectNo304(getClient().findMicroschemas(new PagingParameters().setPage(2)), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer schema = microschemaContainers().get("vcard");

			MeshResponse<Microschema> response = getClient().findMicroschemaByUuid(schema.getUuid()).invoke();
			latchFor(response);
			String etag = schema.getETag(getMockedInternalActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<Microschema> request = getClient().findMicroschemaByUuid(schema.getUuid());
			assertThat(expect304(request, etag, true)).contains(etag);

			// The node has no node reference and thus expanding will not affect the etag
			assertThat(expect304(getClient().findMicroschemaByUuid(schema.getUuid(), new NodeParameters().setExpandAll(true)), etag, true))
					.contains(etag);

			// Assert that adding bogus query parameters will not affect the etag
			expect304(getClient().findMicroschemaByUuid(schema.getUuid(), new NodeParameters().setExpandAll(false)), etag, true);
			expect304(getClient().findMicroschemaByUuid(schema.getUuid(), new NodeParameters().setExpandAll(true)), etag, true);
		}

	}

}
