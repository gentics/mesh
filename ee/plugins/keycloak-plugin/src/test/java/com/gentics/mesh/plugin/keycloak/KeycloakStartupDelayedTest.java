package com.gentics.mesh.plugin.keycloak;

import java.time.Duration;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.Wait;

import com.gentics.mesh.plugin.keycloak.config.KeycloakPluginConfig;
import com.gentics.mesh.test.docker.KeycloakContainer;
import com.gentics.mesh.test.util.TestUtils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.WebClient;

public class KeycloakStartupDelayedTest extends AbstractKeycloakPluginTest {

	protected static Vertx vertx;
	protected static WebClient realClient;
	protected static HttpServer proxy;

	protected static boolean timeout = true;
	protected static final long TIMEOUT = Duration.ofSeconds(10).toMillis();

	private static final Logger log = LoggerFactory.getLogger(KeycloakStartupDelayedTest.class);

	static {
		realmUrlPrefix = StringUtils.EMPTY;
		vertx = Vertx.builder().build();
	}

	@ClassRule
	public static final TestRule initKeycloak() {
		keycloak = new KeycloakContainer("/keycloak/realm.json", "keycloak/keycloak", "22.0.5", Arrays.asList("start-dev"), true)
				.waitingFor(Wait.forHttp("/realms/master-test"));
		realClient = WebClient.create(vertx);
		// Create a dummy server which just blocks on every in-bound request for 1 second
		proxy = vertx.createHttpServer(new HttpServerOptions().setPort(TestUtils.getRandomPort()));
		proxy.requestHandler(rh -> {
			try {
				if (timeout) {
					Thread.sleep(TIMEOUT);
					timeout = false;
				}
				HttpRequest<Buffer> realRequest = realClient.request(
						rh.method(), 
						keycloak.getFirstMappedPort(), keycloak.getContainerIpAddress(), 
						rh.uri()
					).putHeaders(rh.headers());
				realRequest.queryParams().addAll(rh.params());
				if (HttpMethod.POST == rh.method() || HttpMethod.PUT == rh.method()) {
					rh.bodyHandler(body -> realRequest.sendBuffer(body).andThen(rs -> {
						log.info(rh.toString() + " body sent");
						rh.response().end(rs.result().body());
					}));
				} else {
					realRequest.send().andThen(rs -> {
						log.info(rh.toString() + " sent");
						rh.response().end(rs.result().body());
					});
				}
			} catch (InterruptedException e) {
			}
		});
		proxy.rxListen().subscribe();

		return keycloak;
	} ;

	@BeforeClass
	public static void configurePlugin() {
		KeycloakTestPlugin.config = new KeycloakPluginConfig()
			.setRetryPeriodSeconds(5)
			.setRealmName("master-test")
			.setRealmUrlPrefix(realmUrlPrefix)
			.setUrl("http://localhost:" + proxy.actualPort())
			.setLoadPublicKeys(true);
	}
}
