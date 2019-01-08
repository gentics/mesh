package com.gentics.mesh.core.webroot.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.expectException;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NavRootEndpointTest extends AbstractMeshTest {

	/**
	 * Test reading a navigation concurrently.
	 */
	@Test
	public void testReadMultithreaded() {
		try (Tx tx = tx()) {
			int nJobs = 200;
			String path = "/";

			List<MeshResponse<NavigationResponse>> futures = new ArrayList<>();
			for (int i = 0; i < nJobs; i++) {
				futures.add(client().navroot(PROJECT_NAME, path, new NodeParametersImpl().setLanguages("en", "de")).invoke());
			}

			for (MeshResponse<NavigationResponse> fut : futures) {
				latchFor(fut);
				assertSuccess(fut);
				assertThat(fut.result()).isValid(7).hasDepth(3);
			}
		}
	}

	/**
	 * Test reading a navigation using a valid path.
	 */
	@Test
	public void testReadNavWithValidPath() {
		try (Tx tx = tx()) {
			String path = "/News/2015";
			NavigationResponse response = call(() -> client().navroot(PROJECT_NAME, path, new NavigationParametersImpl().setMaxDepth(10)));
			assertThat(response).hasDepth(0).isValid(1);
		}
	}

	/**
	 * Test reading a navigation using a valid path.
	 */
	@Test
	public void testReadNavWithValidPath2() {
		String path = "/News/2015/";
		NavigationResponse response = call(() -> client().navroot(PROJECT_NAME, path, new NavigationParametersImpl().setMaxDepth(10)));
		assertThat(response).isValid(1).hasDepth(0);
	}

	/**
	 * Test reading a navigation using the project basenode as root element.
	 */
	@Test
	public void testReadNavForBasenode() {
		try (Tx tx = tx()) {

			// for (NodeGraphFieldContainer container : project().getBaseNode().getGraphFieldContainers()) {
			// System.out.println(container.isPublished(project().getLatestBranch().getUuid()));
			// }
			String path = "/";
			NavigationResponse response = client().navroot(PROJECT_NAME, path, new NavigationParametersImpl().setMaxDepth(10)).blockingGet();
			assertThat(response).isValid(7).hasDepth(3);
			assertEquals("The root element of the navigation did not contain the project basenode uuid.", project().getBaseNode().getUuid(),
				response.getUuid());
		}
	}

	/**
	 * Test reading a navigation using a bogus path.
	 */
	@Test
	public void testReadNavWithInvalidPath() {
		try (Tx tx = tx()) {
			String path = "/blub";
			MeshResponse<NavigationResponse> future = client().navroot(PROJECT_NAME, path).invoke();
			latchFor(future);
			expectException(future, NOT_FOUND, "node_not_found_for_path", "/blub");
		}
	}

	/**
	 * Test reading a navigation by using a path that points to a content instead of a container.
	 */
	@Test
	public void testReadNavWithPathToContent() {
		try (Tx tx = tx()) {
			String path = "/News/2015/News_2015.en.html";
			call(() -> client().navroot(PROJECT_NAME, path, new NodeParametersImpl().setLanguages("en", "de")), BAD_REQUEST, "navigation_error_no_container");
		}
	}
}
