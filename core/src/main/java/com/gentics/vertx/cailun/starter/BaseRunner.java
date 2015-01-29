package com.gentics.vertx.cailun.starter;

import io.vertx.core.Vertx;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.neo4j.kernel.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.vertx.cailun.auth.Neo4jSpringConfiguration;

@SuppressWarnings("deprecation")
public abstract class BaseRunner {

	private static final Logger log = LoggerFactory.getLogger(BaseRunner.class);

	public BaseRunner() throws Exception {
		run();
	}

	/**
	 * Main entry point for cailun. This method will initialize the spring context and deploy mandatory verticles and extensions.
	 * 
	 * @throws Exception
	 */
	private void run() throws Exception {
		printProductInformation();
		// For testing - We cleanup all the data. The customer module contains a class that will setup a fresh graph each startup.
		FileUtils.deleteDirectory(new File("/tmp/graphdb"));
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Neo4jSpringConfiguration.class)) {
			Vertx vertx = ctx.getBean(Vertx.class);
			SpringVerticleFactory.setParentContext(ctx);
			ctx.start();
			deployCustom(vertx);
			ctx.registerShutdownHook();
			while(true) {
				try {
				Thread.sleep(1000);
				} catch (Exception e) {
				e.printStackTrace();
				}
			}
		}

	}

	protected abstract void deployCustom(Vertx vertx) throws InterruptedException;

	private void printProductInformation() {
		log.info("#################################################");
		log.info(infoLine("CaiLun Version " + getVersion()));
		log.info(infoLine("Gentics Software GmbH"));
		log.info("#-----------------------------------------------#");
		log.info(infoLine("Neo4j Version : " + Version.getKernel().getReleaseVersion()));
		log.info(infoLine("Vert.x Version: " + getVertxVersion()));
		log.info("#################################################");
	}

	private String getVertxVersion() {
		// TODO extract from pom.xml metadata?
		return "3.0.0-milestone2";
	}

	private static String infoLine(String text) {
		return "# " + StringUtils.rightPad(text, 45) + " #";
	}

	private static String getVersion() {
		// TODO extract from pom.xml metadata?
		return "0.0.1";
	}

}
