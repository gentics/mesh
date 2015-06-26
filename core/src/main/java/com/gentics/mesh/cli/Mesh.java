package com.gentics.mesh.cli;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.mesh.etc.ConfigurationLoader;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.config.MeshConfiguration;
import com.gentics.mesh.etc.config.MeshConfigurationException;

public class Mesh {

	private static final Logger log;

	private MeshCustomLoader<Vertx> verticleLoader;

	private static Mesh instance;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		log = LoggerFactory.getLogger(Mesh.class);
	}

	public static Mesh mesh() {
		if (instance == null) {
			instance = new Mesh();
		}
		return instance;
	}

	private Mesh() {
	}

	public static void main(String[] args) throws Exception {
		// TODO errors should be handled by a logger
		Mesh mesh = Mesh.mesh();
		mesh.handleArguments(args);
		mesh.run();
	}

	public void run() throws Exception {
		run(ConfigurationLoader.createOrloadConfiguration(), null);
	}

	public void run(MeshConfiguration config) throws Exception {
		run(config, null);
	}

	public void run(Runnable startupHandler) throws Exception {
		run(ConfigurationLoader.createOrloadConfiguration(), startupHandler);
	}

	/**
	 * Main entry point for mesh. This method will initialize the spring context and deploy mandatory verticles and extensions.
	 * 
	 * @param conf
	 *            The mesh configuration that should be used.
	 * @param startupHandler
	 * @throws Exception
	 */
	public void run(MeshConfiguration conf, Runnable startupHandler) throws Exception {
		if (conf == null) {
			throw new MeshConfigurationException("Configuration is null or not valid.");
		}
		MeshSpringConfiguration.setConfiguration(conf);
		printProductInformation();

		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MeshSpringConfiguration.class)) {
			SpringVerticleFactory.setParentContext(ctx);
			BootstrapInitializer initalizer = ctx.getBean(BootstrapInitializer.class);
			ctx.start();
			initalizer.init(conf, verticleLoader);
			ctx.registerShutdownHook();

			if (startupHandler != null) {
				startupHandler.run();
			} else {
				dontExit();
			}

		}
	}

	/**
	 * Handle command line arguments.
	 * 
	 * @param args
	 * @throws ParseException
	 */
	public void handleArguments(String[] args) throws ParseException {
		// TODO WIP
		// create Options object
		// Options options = new Options();
		//
		// // add t option
		// options.addOption("t", false, "display current time");
		// CommandLineParser parser = new BasicParser();
		// CommandLine cmd = parser.parse(options, args);
		// if (cmd.hasOption("t")) {
		// System.out.println("OK t");
		// } else {
		// System.err.println("No option t");
		// }

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

	private void printProductInformation() {
		log.info("#################################################");
		log.info(infoLine("Mesh Version " + getVersion()));
		log.info(infoLine("Gentics Software GmbH"));
		log.info("#-----------------------------------------------#");
		//log.info(infoLine("Neo4j Version : " + Version.getKernel().getReleaseVersion()));
		log.info(infoLine("Vert.x Version: " + getVertxVersion()));
		log.info("#################################################");
	}

	private String getVertxVersion() {
		// TODO extract from pom.xml metadata?
		// Package pack = Vertx.class.getPackage();
		// return pack.getImplementationVersion();
		return new io.vertx.core.Starter().getVersion();
	}

	private static String infoLine(String text) {
		return "# " + StringUtils.rightPad(text, 45) + " #";
	}

	private static String getVersion() {
		Package pack = Mesh.class.getPackage();
		return pack.getImplementationVersion();
	}

	/**
	 * Set a custom verticle loader that will be invoked once all major components have been initialized.
	 * 
	 * @param verticleLoader
	 */
	public void setCustomLoader(MeshCustomLoader<Vertx> verticleLoader) {
		this.verticleLoader = verticleLoader;
	}

}
