package com.gentics.cailun.demo;

import static com.gentics.cailun.util.DeploymentUtils.deployAndWait;

import com.gentics.cailun.cli.BaseRunner;
import com.gentics.cailun.core.verticle.AuthenticationVerticle;
import com.gentics.cailun.core.verticle.PageVerticle;
import com.gentics.cailun.core.verticle.TagVerticle;
import com.gentics.cailun.demo.verticle.CustomerVerticle;
import com.gentics.cailun.verticle.admin.AdminVerticle;
import com.gentics.vertx.cailun.nav.NavigationVerticle;
import com.gentics.vertx.cailun.tagcloud.TagCloudVerticle;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 * 
 * @author johannes2
 *
 */
public class Runner {

	public static void main(String[] args) throws Exception {
		new BaseRunner(args, (vertx) -> {
			deployAndWait(vertx, CustomerVerticle.class);
			deployAndWait(vertx, AdminVerticle.class);
			deployAndWait(vertx, AuthenticationVerticle.class);
			// DeploymentOptions options = new DeploymentOptions();
			// vertx.deployVerticle("service:com.gentics.vertx:cailun-rest-navigation:0.1.0-SNAPSHOT",options, dh -> {
			// if (dh.failed()) {
			// System.out.println(dh.cause());
			// }
			// });
				deployAndWait(vertx, NavigationVerticle.class);
				deployAndWait(vertx, TagCloudVerticle.class);
				deployAndWait(vertx, PageVerticle.class);
				deployAndWait(vertx, TagVerticle.class);
				// deployAndWait(vertx, "", "TestJSVerticle.js");
			});
	}

}
