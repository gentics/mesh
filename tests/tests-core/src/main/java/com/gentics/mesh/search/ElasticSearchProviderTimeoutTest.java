package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.MeshCoreOptionChanger.RANDOM_ES_PORT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.WebClient;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.PROJECT_AND_NODE, startServer = true, optionChanger = RANDOM_ES_PORT)
public class ElasticSearchProviderTimeoutTest extends AbstractMeshTest {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProviderTimeoutTest.class);

	private static HttpServer server;
	private static WebClient realClient;
	private static boolean timeout = false;

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
		realClient = WebClient.create(vertx);
		// Create a dummy server which just blocks on every in-bound request for 1 second
		server = vertx.createHttpServer(new HttpServerOptions().setPort(port));
		server.requestHandler(rh -> {
			try {
				if (timeout) {
					Thread.sleep(Duration.ofSeconds(16).toMillis());
				}
				HttpRequest<Buffer> realRequest = realClient.request(
						rh.method(), 
						testContext.elasticsearchContainer().getMappedPort(9200), 
						testContext.elasticsearchContainer().getHost(), 
						rh.uri()
					).putHeaders(rh.headers());
				realRequest.queryParams().addAll(rh.params());
				if (HttpMethod.POST == rh.method() || HttpMethod.PUT == rh.method()) {
					rh.bodyHandler(body -> realRequest.sendBuffer(body, rs -> {
						log.info(rh.toString() + " body sent");
						rh.response().end(rs.result().body());
					}));
				} else {
					realRequest.send(rs -> {
						log.info(rh.toString() + " sent");
						rh.response().end(rs.result().body());
					});
				}
			} catch (InterruptedException e) {
			}
		});
		server.rxListen().subscribe();
	}

	@Before
	public void set() {
		timeout = true;
	}

	@After
	public void reset() {
		timeout = false;
	}

	@Test
	public void testHugeBatch() throws IOException {
		String username = "testuser";
		for (int i = 0; i < 1200; i++) {
			createUser(username + i);
		}
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

	@Test
	public void testResumeQueriesAfterBlackout() throws IOException, InterruptedException {
		// black out!
		String json = getESText("userWildcard.es");
		call(() -> client().searchUsers(json), INTERNAL_SERVER_ERROR, "search_error_timeout");
		// reset
		timeout = false;
		Thread.sleep(Duration.ofSeconds(16).toMillis());
		UserListResponse response = call(() -> client().searchUsers(json));
		assertThat(response.getData()).isNotNull();
	}
}