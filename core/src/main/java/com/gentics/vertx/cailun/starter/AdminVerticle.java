package com.gentics.vertx.cailun.starter;

import static com.gentics.vertx.cailun.starter.DeploymentUtils.deployAndWait;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.Handler;
import io.vertx.ext.apex.core.RoutingContext;
import io.vertx.ext.apex.core.Session;

import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.page.PageRepository;
import com.gentics.vertx.cailun.page.model.Page;
import com.gentics.vertx.cailun.perm.model.GenericPermission;
import com.gentics.vertx.cailun.perm.model.PermissionSet;
import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class AdminVerticle extends AbstractCailunRestVerticle {

	@Autowired
	PageRepository pageRepository;

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
			// org.apache.shiro.mgt.SecurityManager manager = SecurityUtils.getSecurityManager();
			// Subject subject = SecurityUtils.getSubject();

			List<Page> pages = pageRepository.findAllPages();
			Page pageToBeUsed = null;
			for (Page page : pages) {
				if ("Index With Perm".equalsIgnoreCase(page.getName())) {
					pageToBeUsed = page;
				}
			}

			getAuthService().hasPermission(sess.getPrincipal(), new GenericPermission(pageToBeUsed, PermissionSet.MODIFY), rh -> {
				System.out.println("Has Perm: " + rh.result());
				if (rh.result()) {
					rc.response().end("Welcome to the protected resource!");
				} else {
					rc.response().end("Protected content!");
				}
			});
			// securityConfig.authService().hasPermission(sess.getPrincipal(), "create", rh -> {
			// System.out.println("Has Perm: " + rh.result());
			// if (rh.result()) {
			// rc.response().end("Welcome to the protected resource!");
			// } else {
			// rc.response().end("Protected content!");
			//
			// }
			// });

		};



		// addVerticleHandler();
		// addServiceHandler();

		// route("/protected").handler(authHandler);
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
