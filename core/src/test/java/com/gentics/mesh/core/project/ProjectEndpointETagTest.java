package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = PROJECT, startServer = true)
public class ProjectEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().findProjects());
			callETag(() -> client().findProjects(), etag, true, 304);
			callETag(() -> client().findProjects(new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			String actualETag = callETag(() -> client().findProjectByUuid(projectUuid()));
			String etag = project().getETag(mockActionContext());
			assertEquals(etag, actualETag);

			// Check whether 304 is returned for correct etag
			assertEquals(etag, callETag(() -> client().findProjectByUuid(project().getUuid()), etag, true, 304));

			// The project has no node reference and thus expanding will not affect the etag
			assertEquals(etag, callETag(() -> client().findProjectByUuid(project().getUuid()), etag, true, 304));

			// Assert that adding bogus query parameters will not affect the etag
			callETag(() -> client().findProjectByUuid(project().getUuid(), new NodeParametersImpl().setExpandAll(false)), etag, true, 304);
			callETag(() -> client().findProjectByUuid(project().getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304);
		}

	}

}
