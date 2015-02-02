package com.gentics.cailun.demo;

import static com.gentics.cailun.util.DeploymentUtils.deployAndWait;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.gentics.cailun.cli.CaiLun;
import com.gentics.cailun.core.verticle.AuthenticationVerticle;
import com.gentics.cailun.core.verticle.PageVerticle;
import com.gentics.cailun.core.verticle.TagVerticle;
import com.gentics.cailun.demo.verticle.CustomerVerticle;
import com.gentics.cailun.nav.NavigationVerticle;
import com.gentics.cailun.tagcloud.TagCloudVerticle;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 * 
 * @author johannes2
 *
 */
public class DemoRunner {

	public static void main(String[] args) throws Exception {

		// For testing - We cleanup all the data. The customer module contains a class that will setup a fresh graph each startup.
		FileUtils.deleteDirectory(new File("/tmp/graphdb"));

		CaiLun cailun = CaiLun.getInstance();

		cailun.setCustomLoader((vertx) -> {
			deployAndWait(vertx, CustomerVerticle.class);
			deployAndWait(vertx, AuthenticationVerticle.class);
			deployAndWait(vertx, NavigationVerticle.class);
			deployAndWait(vertx, TagCloudVerticle.class);
			deployAndWait(vertx, PageVerticle.class);
			deployAndWait(vertx, TagVerticle.class);
			//deployAndWait(vertx, StaticContentVerticle.class);
			//deployAndWait(vertx, AdminGUIVerticle.class);
			
		});
//			// DeploymentOptions options = new DeploymentOptions();
//			// vertx.deployVerticle("service:com.gentics.vertx:cailun-rest-navigation:0.1.0-SNAPSHOT",options, dh -> {
//			// if (dh.failed()) {
//			// System.out.println(dh.cause());
//			// }
//			// });
//			// deployAndWait(vertx, "", "TestJSVerticle.js");
//		});
		cailun.run();

	}

}
