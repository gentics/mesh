package com.gentics.mesh.core.schema;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class SchemaEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().findSchemas());
			assertNotNull(etag);

			callETag(() -> client().findSchemas(), etag, true, 304);
			callETag(() -> client().findSchemas(new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");

			String responseTag = callETag(() -> client().findSchemaByUuid(schema.getUuid()));
			String etag = schema.getETag(mockActionContext());
			assertEquals(etag, responseTag);

			// Check whether 304 is returned for correct etag
			assertThat(callETag(() -> client().findSchemaByUuid(schema.getUuid()), etag, true, 304)).contains(etag);

			// The node has no node reference and thus expanding will not affect the etag
			assertThat(callETag(() -> client().findSchemaByUuid(schema.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304))
					.contains(etag);

			// Assert that adding bogus query parameters will not affect the etag
			callETag(() -> client().findSchemaByUuid(schema.getUuid(), new NodeParametersImpl().setExpandAll(false)), etag, true, 304);
			callETag(() -> client().findSchemaByUuid(schema.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304);
		}

	}

}
