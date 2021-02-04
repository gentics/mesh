package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.context.MeshOptionChanger.RANDOM_ES_PORT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.PROJECT_AND_NODE, startServer = true, optionChanger = RANDOM_ES_PORT)
public class ElasticSearchProviderTimeoutTest extends AbstractMeshTest {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProviderTimeoutTest.class);

	/**
	 * Use the vert.x server which answers requests after 16s instead.
	 * 
	 * @throws IOException
	 */
	@BeforeClass
	public static void setupTimeoutServer() throws IOException {
		Vertx vertx = new Vertx(testContext.getVertx());
		ElasticSearchOptions options = testContext.getOptions().getSearchOptions();
		String url = options.getUrl();
		int port = new URL(url).getPort();
		// Create a dummy server which just blocks on every in-bound request for 1 second
		HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(port));
		server.requestHandler(rh -> {
			// Don't block validation requests.
			if (rh.absoluteURI().indexOf("_template/validation") > 0) {
				rh.response().end(new JsonObject().encodePrettily());
			} else {
				log.info("Waiting for 16 second to answer request: " + rh.absoluteURI());
				vertx.setTimer(Duration.ofSeconds(3).toMillis(), th -> rh.response().end());
			}
		});
		server.rxListen().blockingGet();

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