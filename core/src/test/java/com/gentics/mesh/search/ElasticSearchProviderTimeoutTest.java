package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.time.Duration;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
@MeshTestSetting(elasticsearch = CONTAINER, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class ElasticSearchProviderTimeoutTest extends AbstractMeshTest {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProviderTimeoutTest.class);

	/**
	 * Use the vert.x server which answers requests after 16s instead.
	 * 
	 * @throws IOException
	 */
	@BeforeClass
	public static void setupTimeoutServer() throws IOException {
		Vertx vertx = new Vertx(Mesh.mesh().getVertx());
		ElasticSearchProvider provider = ((ElasticSearchProvider) MeshInternal.get().searchProvider());
		ElasticSearchOptions options = provider.getOptions();

		// Create a dummy server which just blocks on every in-bound request for 1 second
		HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(0));
		server.requestHandler(rh -> {
			// Don't block validation requests.
			if (rh.absoluteURI().indexOf("_template/validation") > 0) {
				rh.response().end(new JsonObject().encodePrettily());
			} else {
				log.info("Waiting for 16 second to answer request: " + rh.absoluteURI());
				vertx.setTimer(Duration.ofSeconds(16).toMillis(), th -> rh.response().end());
			}
		});
		server.rxListen().blockingGet();

		// Set some bogus connection details and restart the provider
		options.setTimeout(500L).setUrl(null);
		options.setUrl("http://localhost:" + server.actualPort());
		provider.stop();
		provider.start();
	}

	@Test
	public void testDocumentCreation() throws IOException {
		String username = "testuser42a";
		try (Tx tx = tx()) {
			createUser(username);
		}
	}

	@Test
	public void testSearchQuery() throws IOException {
		String json = getESText("userWildcard.es");
		call(() -> client().searchUsers(json), INTERNAL_SERVER_ERROR, "search_error_timeout");
	}
}