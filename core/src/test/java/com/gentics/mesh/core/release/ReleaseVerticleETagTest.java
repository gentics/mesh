package com.gentics.mesh.core.release;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

import io.vertx.core.AbstractVerticle;

public class ReleaseVerticleETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<ReleaseListResponse> response = getClient().findReleases(PROJECT_NAME).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(getClient().findReleases(PROJECT_NAME), etag, true);
			expectNo304(getClient().findReleases(PROJECT_NAME, new PagingParameters().setPage(2)), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			Release release = project().getLatestRelease();
			MeshResponse<ReleaseResponse> response = getClient().findReleaseByUuid(PROJECT_NAME, release.getUuid()).invoke();
			latchFor(response);
			String etag = release.getETag(getMockedInternalActionContext());
			assertThat(response.getResponse().getHeader(ETAG)).contains(etag);

			// Check whether 304 is returned for correct etag
			MeshRequest<ReleaseResponse> request = getClient().findReleaseByUuid(PROJECT_NAME, release.getUuid());
			assertThat(expect304(request, etag, true)).contains(etag);

			// Assert that adding bogus query parameters will not affect the etag
			expect304(getClient().findReleaseByUuid(PROJECT_NAME, release.getUuid(), new NodeParameters().setExpandAll(false)), etag, true);
			expect304(getClient().findReleaseByUuid(PROJECT_NAME, release.getUuid(), new NodeParameters().setExpandAll(true)), etag, true);
		}

	}

}
