package com.gentics.vertx.cailun.starter;

import static com.gentics.vertx.cailun.starter.DeploymentUtils.deployAndWait;
import io.vertx.core.Vertx;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.neo4j.kernel.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.vertx.cailun.base.AuthenticationVerticle;
import com.gentics.vertx.cailun.base.TagVerticle;
import com.gentics.vertx.cailun.demo.CustomerVerticle;
import com.gentics.vertx.cailun.page.PageVerticle;

@SuppressWarnings("deprecation")
public class Runner {

	private static final Logger log = LoggerFactory.getLogger(Runner.class);

	public static void main(String[] args) throws IOException, InterruptedException {

		printProductInformation();
		// For testing - We cleanup all the data. The customer module contains a class that will setup a fresh graph each startup.
		FileUtils.deleteDirectory(new File("/tmp/graphdb"));
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Neo4jSpringConfiguration.class)) {
			Vertx vertx = ctx.getBean(Vertx.class);
			SpringVerticleFactory.setParentContext(ctx);
			ctx.start();
			deployAndWait(vertx, CustomerVerticle.class);
			deployAndWait(vertx, AdminVerticle.class);
			deployAndWait(vertx, AuthenticationVerticle.class);
			for (int i = 0; i < 1; i++) {
				deployAndWait(vertx, PageVerticle.class);
				deployAndWait(vertx, TagVerticle.class);
			}

			ctx.registerShutdownHook();
			System.in.read();
		}
	}

	private static void printProductInformation() {
		// TODO
		log.info("#################################################");
		log.info(infoLine("CaiLun Version " + getVersion()));
		log.info(infoLine("Gentics Software GmbH"));
		log.info("#-----------------------------------------------#");
		log.info(infoLine("Neo4j Version : " + Version.getKernel().getReleaseVersion()));
		log.info(infoLine("Vert.x Version: 3.0.0-SNAPSHOT"));
		log.info("#################################################");
	}

	private static String infoLine(String text) {
		return "# " + StringUtils.rightPad(text, 45) + " #";
	}

	private static String getVersion() {
		// TODO extract from metadata?
		return "0.0.1";
	}

}
