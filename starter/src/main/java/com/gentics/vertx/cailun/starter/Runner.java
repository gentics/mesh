package com.gentics.vertx.cailun.starter;

import static com.gentics.vertx.cailun.starter.DeploymentUtils.deployAndWait;
import io.vertx.core.Vertx;

import com.gentics.vertx.cailun.base.AuthenticationVerticle;
import com.gentics.vertx.cailun.base.TagVerticle;
import com.gentics.vertx.cailun.demo.CustomerVerticle;
import com.gentics.vertx.cailun.nav.NavigationVerticle;
import com.gentics.vertx.cailun.page.PageVerticle;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 * 
 * @author johannes2
 *
 */
public class Runner extends BaseRunner {

	public Runner() throws Exception {
		super();
	}

	public static void main(String[] args) throws Exception {
		new Runner();
	}

	@Override
	protected void deployCustom(Vertx vertx) throws InterruptedException {
		deployAndWait(vertx, CustomerVerticle.class);
		deployAndWait(vertx, AdminVerticle.class);
		deployAndWait(vertx, AuthenticationVerticle.class);
//		DeploymentOptions options = new DeploymentOptions();
//		vertx.deployVerticle("service:com.gentics.vertx:cailun-rest-navigation:0.1.0-SNAPSHOT",options, dh -> {
//			if (dh.failed()) {
//				System.out.println(dh.cause());
//			}
//		});
		deployAndWait(vertx, NavigationVerticle.class);
		deployAndWait(vertx, PageVerticle.class);
		deployAndWait(vertx, TagVerticle.class);

	}

}
