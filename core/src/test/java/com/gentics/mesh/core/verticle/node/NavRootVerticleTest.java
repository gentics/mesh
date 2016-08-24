package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.verticle.navroot.NavRootVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NavigationParameters;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;

public class NavRootVerticleTest extends AbstractIsolatedRestVerticleTest {

	private NavRootVerticle navRootVerticle;

	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(navRootVerticle);
		list.add(nodeVerticle);
		return list;
	}

	/**
	 * Test reading a navigation concurrently.
	 */
	@Test
	public void testReadMultithreaded() {
		try (NoTx noTx = db.noTx()) {
			int nJobs = 200;
			String path = "/";

			List<MeshResponse<NavigationResponse>> futures = new ArrayList<>();
			for (int i = 0; i < nJobs; i++) {
				futures.add(getClient().navroot(PROJECT_NAME, path, new NodeParameters().setLanguages("en", "de")).invoke());
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
		try (NoTx noTx = db.noTx()) {
			String path = "/News/2015";
			MeshResponse<NavigationResponse> future = getClient().navroot(PROJECT_NAME, path, new NavigationParameters().setMaxDepth(10)).invoke();
			latchFor(future);
			assertSuccess(future);
			assertThat(future.result()).hasDepth(0).isValid(1);
		}
	}

	/**
	 * Test reading a navigation using a valid path.
	 */
	@Test
	public void testReadNavWithValidPath2() {
		try (NoTx noTx = db.noTx()) {
			String path = "/News/2015/";
			MeshResponse<NavigationResponse> future = getClient().navroot(PROJECT_NAME, path, new NavigationParameters().setMaxDepth(10)).invoke();
			latchFor(future);
			assertSuccess(future);
			assertThat(future.result()).isValid(1).hasDepth(0);
		}
	}

	/**
	 * Test reading a navigation using the project basenode as root element.
	 */
	@Test
	public void testReadNavForBasenode() {
		try (NoTx noTx = db.noTx()) {

//			for (NodeGraphFieldContainer container : project().getBaseNode().getGraphFieldContainers()) {
//				System.out.println(container.isPublished(project().getLatestRelease().getUuid()));
//			}
			String path = "/";
			MeshResponse<NavigationResponse> future = getClient().navroot(PROJECT_NAME, path, new NavigationParameters().setMaxDepth(10)).invoke();
			latchFor(future);
			assertSuccess(future);
			assertThat(future.result()).isValid(7).hasDepth(3);
			assertEquals("The root element of the navigation did not contain the project basenode uuid.", project().getBaseNode().getUuid(),
					future.result().getRoot().getUuid());
		}
	}

	/**
	 * Test reading a navigation using a bogus path.
	 */
	@Test
	public void testReadNavWithInvalidPath() {
		try (NoTx noTx = db.noTx()) {
			String path = "/blub";
			MeshResponse<NavigationResponse> future = getClient().navroot(PROJECT_NAME, path).invoke();
			latchFor(future);
			expectException(future, NOT_FOUND, "node_not_found_for_path", "/blub");
		}
	}

	/**
	 * Test reading a navigation by using a path that points to a content instead of a container.
	 */
	@Test
	public void testReadNavWithPathToContent() {
		try (NoTx noTx = db.noTx()) {
			String path = "/News/2015/News_2015.en.html";
			MeshResponse<NavigationResponse> future = getClient().navroot(PROJECT_NAME, path, new NodeParameters().setLanguages("en", "de")).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "navigation_error_no_container");
		}
	}
}
