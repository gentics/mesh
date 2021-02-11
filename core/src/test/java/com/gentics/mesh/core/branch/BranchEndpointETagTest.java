package com.gentics.mesh.core.branch;

import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class BranchEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().findBranches(PROJECT_NAME));
			assertNotNull(etag);

			callETag(() -> client().findBranches(PROJECT_NAME), etag, true, 304);
			callETag(() -> client().findBranches(PROJECT_NAME, new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			BranchDao branchDao = tx.branchDao();
			HibBranch branch = project().getLatestBranch();
			String actualEtag = callETag(() -> client().findBranchByUuid(PROJECT_NAME, branch.getUuid()));
			String etag = branchDao.getETag(branch, mockActionContext());
			assertThat(actualEtag).contains(etag);

			// Check whether 304 is returned for correct etag
			assertThat(callETag(() -> client().findBranchByUuid(PROJECT_NAME, branch.getUuid()), etag, true, 304)).contains(etag);

			// Assert that adding bogus query parameters will not affect the etag
			callETag(() -> client().findBranchByUuid(PROJECT_NAME, branch.getUuid(), new NodeParametersImpl().setExpandAll(false)), etag, true, 304);
			callETag(() -> client().findBranchByUuid(PROJECT_NAME, branch.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304);
		}

	}

}
