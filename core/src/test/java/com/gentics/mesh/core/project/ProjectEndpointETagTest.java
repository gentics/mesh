package com.gentics.mesh.core.project;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true)
public class ProjectEndpointETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db().noTx()) {
			MeshResponse<ProjectListResponse> response = client().findProjects().invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(client().findProjects(), etag, true);
			expectNo304(client().findProjects(new PagingParametersImpl().setPage(2)), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db().noTx()) {
			Project project = project();
			MeshResponse<ProjectResponse> response = client().findProjectByUuid(project.getUuid()).invoke();
			latchFor(response);
			String etag = project.getETag(mockActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<ProjectResponse> request = client().findProjectByUuid(project.getUuid());
			assertEquals(etag, expect304(request, etag, true));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag,
					expect304(client().findProjectByUuid(project.getUuid(), new NodeParameters().setExpandAll(true)),
							etag, true));

			// Assert that adding bogus query parameters will not affect the etag
			expect304(client().findProjectByUuid(project.getUuid(), new NodeParameters().setExpandAll(false)), etag,
					true);
			expect304(client().findProjectByUuid(project.getUuid(), new NodeParameters().setExpandAll(true)), etag,
					true);
		}

	}

}
