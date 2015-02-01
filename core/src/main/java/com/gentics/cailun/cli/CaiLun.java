package com.gentics.cailun.cli;

import static com.gentics.cailun.util.DeploymentUtils.deployAndWait;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.VertxSPI;
import io.vertx.spi.cluster.impl.hazelcast.HazelcastClusterManager;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.neo4j.kernel.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.cailun.etc.CaiLunConfiguration;
import com.gentics.cailun.etc.CaiLunConfigurationException;
import com.gentics.cailun.etc.CaiLunCustomLoader;
import com.gentics.cailun.etc.ConfigurationLoader;
import com.gentics.cailun.etc.Neo4jSpringConfiguration;

public class CaiLun {

	private static final Logger log = LoggerFactory.getLogger(CaiLun.class);

	private CaiLunCustomLoader<Vertx> verticleLoader;
	private CaiLunConfiguration configuration;

	private Vertx vertx;
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

	/**
	 * Main entry point for cailun. This method will initialize the spring context and deploy mandatory verticles and extensions.
	 * 
	 * @param conf
	 *            The CailunConfiguration that should be used.
	 * @throws Exception
	 */
	public void run(CaiLunConfiguration conf) throws Exception {
		if (conf == null) {
			throw new CaiLunConfigurationException("Configuration is null or not valid.");
		}
		configuration = conf;
		printProductInformation();
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Neo4jSpringConfiguration.class)) {
			vertx = ctx.getBean(Vertx.class);
			SpringVerticleFactory.setParentContext(ctx);
			if (configuration.isClusterMode()) {
				joinCluster();
			}
			ctx.start();
			loadConfiguredVerticles();
			if (verticleLoader != null) {
				verticleLoader.apply(vertx);
			}
			ctx.registerShutdownHook();
			dontExit();
		}
	}

	public void run() throws Exception {
		run(ConfigurationLoader.createOrloadConfiguration());
	}

	private void loadConfiguredVerticles() {
		for (String verticleName : configuration.getVerticles().keySet()) {
			try {
				log.info("Loading configured verticle {" + verticleName + "}.");
				deployAndWait(vertx, verticleName);
			} catch (InterruptedException e) {
				log.error("Could not load verticle {" + verticleName + "}.", e);
			}
		}

	}

	/**
	 * Use the hazelcast cluster manager to join the cluster of cailun instances.
	 */
	private void joinCluster() {
		HazelcastClusterManager manager = new HazelcastClusterManager();
		manager.setVertx((VertxSPI) vertx);
		manager.join(rh -> {
			if (!rh.succeeded()) {
				log.error("Error while joining cailun cluster.", rh.cause());
			}
		});
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
