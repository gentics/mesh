package com.gentics.cailun.cli;

import io.vertx.core.Vertx;

import java.io.File;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.neo4j.kernel.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.cailun.auth.Neo4jSpringConfiguration;

public class BaseRunner {

	private static final Logger log = LoggerFactory.getLogger(BaseRunner.class);

	public BaseRunner(String[] args, CaiLunCustomLoader<Vertx> verticleLoader) throws Exception {
		run(args, verticleLoader);
	}

	/**
	 * Main entry point for cailun. This method will initialize the spring context and deploy mandatory verticles and extensions.
	 * 
	 * @param args
	 * @param verticleLoader
	 * 
	 * @throws Exception
	 */
	private void run(String[] args, CaiLunCustomLoader<Vertx> verticleLoader) throws Exception {
		handleArguments(args);

		printProductInformation();
		// For testing - We cleanup all the data. The customer module contains a class that will setup a fresh graph each startup.
		FileUtils.deleteDirectory(new File("/tmp/graphdb"));
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Neo4jSpringConfiguration.class)) {
			Vertx vertx = ctx.getBean(Vertx.class);
			SpringVerticleFactory.setParentContext(ctx);
			ctx.start();
			verticleLoader.apply(vertx);
			ctx.registerShutdownHook();
			dontExit();
		}

	}

	private void handleArguments(String[] args) throws ParseException {
		// create Options object
		Options options = new Options();

		// add t option
		options.addOption("t", false, "display current time");
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		if (cmd.hasOption("t")) {
			System.out.println("OK t");
		} else {
			System.err.println("No option t");
		}

	}

	private void dontExit() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("deprecation")
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
