package com.gentics.mesh.search;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.search.ElasticSearchHost;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class ElasticSearchProviderTimeoutTest extends AbstractMeshTest {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProviderTimeoutTest.class);

	@Test
	public void testSimpleQuerySearch() throws IOException {

		Vertx vertx = new Vertx(Mesh.mesh().getVertx());
		ElasticSearchProvider provider = ((ElasticSearchProvider) searchProvider());
		ElasticSearchOptions options = provider.getOptions();

		// Create a dummy server which just blocks on every in-bound request for 1 second
		HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(0));
		server.requestHandler(rh -> {
			log.info("Waiting for 1 second to answer request: " + rh.absoluteURI());
			vertx.setTimer(6000, th -> rh.response().end());
		});
		server.rxListen().blockingGet();

		// Set some bogus connection details and restart the provider
		options.setTimeout(500L).getHosts().clear();
		options.getHosts().add(new ElasticSearchHost().setHostname("localhost").setPort(server.actualPort()));
		provider.stop();
		provider.start(false);

		String username = "testuser42a";
		try (Tx tx = tx()) {
			createUser(username);
		}
	}
}