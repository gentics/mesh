package com.gentics.vertx.cailun.starter;

import static com.gentics.vertx.cailun.starter.DeploymentUtils.deployAndWait;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.Handler;
import io.vertx.ext.apex.addons.AuthHandler;
import io.vertx.ext.apex.addons.BasicAuthHandler;
import io.vertx.ext.apex.addons.LocalSessionStore;
import io.vertx.ext.apex.addons.SessionHandler;
import io.vertx.ext.apex.core.BodyHandler;
import io.vertx.ext.apex.core.CookieHandler;
import io.vertx.ext.apex.core.RoutingContext;
import io.vertx.ext.apex.core.Session;
import io.vertx.ext.apex.core.SessionStore;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.auth.SecurityConfiguration;
import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class AdminVerticle extends AbstractCailunRestVerticle {

	@Autowired
	SecurityConfiguration securityConfig;

	public AdminVerticle() {
		super("admin");
	}

	@Override
	public void start() throws Exception {
		super.start();

		Handler<RoutingContext> handler = rc -> {
			Session sess = rc.session();
			System.out.println(sess.getPrincipal());
			System.out.println(sess);
			System.out.println(sess.isLoggedIn());
			rc.response().end("Welcome to the protected resource!");
		};

		route().handler(BodyHandler.bodyHandler());
		route().handler(CookieHandler.cookieHandler());
		SessionStore store = LocalSessionStore.localSessionStore(vertx);
		route().handler(SessionHandler.sessionHandler(store));
		AuthHandler authHandler = BasicAuthHandler.basicAuthHandler(securityConfig.authService(), BasicAuthHandler.DEFAULT_REALM);
		route().handler(authHandler);

//		addVerticleHandler();
//		addServiceHandler();

//		route("/protected").handler(authHandler);
		route("/somepage").handler(handler);
	}

	private void addServiceHandler() {
		route("/deployService/:mavenCoordinates").method(GET).handler(rc -> {
			// TODO impl me
				rc.response().end("Deploy " + rc.request().params().get("mavenCoordinates"));
			});

		route("/undeployService/:mavenCoordinates").method(GET).handler(rc -> {
			// TODO impl me
				rc.response().end("Undeploy " + rc.request().params().get("mavenCoordinates"));
			});

	}

	private void addVerticleHandler() {
		route("/deployVerticle/:clazz").method(GET).handler(rc -> {
			String clazz = rc.request().params().get("clazz");
			try {
				String id = deployAndWait(vertx, clazz);
				rc.response().end("Deployed " + clazz + " id: " + id);
			} catch (Exception e) {
				rc.fail(e);
			}
		});

		route("/undeployVerticle/:clazz").method(GET).handler(rc -> {
			// TODO impl me
			rc.response().end("Undeploy " + rc.request().params().get("clazz"));
			});
	}

}
