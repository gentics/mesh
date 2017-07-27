package com.gentics.mesh.core.release;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class ReleaseEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().findReleases(PROJECT_NAME));
			assertNotNull(etag);

			callETag(() -> client().findReleases(PROJECT_NAME), etag, true, 304);
			callETag(() -> client().findReleases(PROJECT_NAME, new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			Release release = project().getLatestRelease();
			String actualEtag = callETag(() -> client().findReleaseByUuid(PROJECT_NAME, release.getUuid()));
			String etag = release.getETag(mockActionContext());
			assertThat(actualEtag).contains(etag);

			// Check whether 304 is returned for correct etag
			assertThat(callETag(() -> client().findReleaseByUuid(PROJECT_NAME, release.getUuid()), etag, true, 304)).contains(etag);

			// Assert that adding bogus query parameters will not affect the etag
			callETag(() -> client().findReleaseByUuid(PROJECT_NAME, release.getUuid(), new NodeParametersImpl().setExpandAll(false)), etag, true, 304);
			callETag(() -> client().findReleaseByUuid(PROJECT_NAME, release.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304);
		}

	}

}
