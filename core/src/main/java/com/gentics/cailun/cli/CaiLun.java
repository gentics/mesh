package com.gentics.cailun.cli;

import io.vertx.core.Vertx;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.neo4j.kernel.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.cailun.etc.CaiLunCustomLoader;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.etc.ConfigurationLoader;
import com.gentics.cailun.etc.config.CaiLunConfiguration;
import com.gentics.cailun.etc.config.CaiLunConfigurationException;

public class CaiLun {

	private static final Logger log = LoggerFactory.getLogger(CaiLun.class);

	private CaiLunCustomLoader<Vertx> verticleLoader;

	private static CaiLun instance;

	public static CaiLun getInstance() {
		if (instance == null) {
			instance = new CaiLun();
		}
		return instance;
	}

	private CaiLun() {
	}

	public static void main(String[] args) throws Exception {
		// TODO errors should be handled by a logger
		CaiLun cailun = CaiLun.getInstance();
		cailun.handleArguments(args);
		cailun.run();
	}

	public void run() throws Exception {
		run(ConfigurationLoader.createOrloadConfiguration(), null);
	}

	public void run(Runnable startupHandler) throws Exception {
		run(ConfigurationLoader.createOrloadConfiguration(), startupHandler);
	}

	/**
	 * Main entry point for cailun. This method will initialize the spring context and deploy mandatory verticles and extensions.
	 * 
	 * @param conf
	 *            The CailunConfiguration that should be used.
	 * @param startupHandler
	 * @throws Exception
	 */
	public void run(CaiLunConfiguration conf, Runnable startupHandler) throws Exception {
		if (conf == null) {
			throw new CaiLunConfigurationException("Configuration is null or not valid.");
		}
		CaiLunSpringConfiguration.setConfiguration(conf);

		printProductInformation();
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CaiLunSpringConfiguration.class)) {
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

	/**
	 * Set a custom verticle loader that will be invoked once all major components have been initialized.
	 * 
	 * @param verticleLoader
	 */
	public void setCustomLoader(CaiLunCustomLoader<Vertx> verticleLoader) {
		this.verticleLoader = verticleLoader;
	}

}
