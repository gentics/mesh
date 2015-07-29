package com.gentics.mesh.cli;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

import java.util.Objects;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;

public class MeshImpl {

	private static final Logger log;

	private MeshCustomLoader<Vertx> verticleLoader;

	private static MeshImpl instance;

	private MeshOptions options;
	private Vertx vertx;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		log = LoggerFactory.getLogger(MeshImpl.class);
	}

	public MeshImpl(MeshOptions options) {
		Objects.requireNonNull(options, "Please specify a valid options object.");
		this.options = options;
	}

	public MeshImpl(MeshOptions options, Vertx vertx) {
		Objects.requireNonNull(options, "Please specify a valid options object.");
		Objects.requireNonNull(vertx, "Please specify a vertx instance.");
		this.options = options;
		this.vertx = vertx;
	}

	public MeshImpl() {
		options = OptionsLoader.createOrloadOptions();
	}

	public static MeshImpl mesh() {
		if (instance == null) {
			instance = new MeshImpl();
		}
		return instance;
	}

	public static MeshImpl mesh(MeshOptions options) {
		if (instance == null) {
			instance = new MeshImpl(options);
		}
		return instance;
	}

	public static MeshImpl mesh(MeshOptions options, Vertx vertx) {
		if (instance == null) {
			instance = new MeshImpl(options, vertx);
		}
		return instance;
	}

	public Vertx getVertx() {
		if (vertx == null) {
			VertxOptions options = new VertxOptions();
			options.setBlockedThreadCheckInterval(1000 * 60 * 60);
			options.setWorkerPoolSize(16);
			vertx = Vertx.vertx(options);
		}
		return vertx;
	}

	public static Vertx vertx() {
		return mesh().getVertx();
	}

	public static void main(String[] args) throws Exception {
		// TODO errors should be handled by a logger
		MeshImpl mesh = MeshImpl.mesh();
		mesh.handleArguments(args);
		mesh.run();
	}

	public void run() throws Exception {
		run(null);
	}

	/**
	 * Main entry point for mesh. This method will initialize the spring context and deploy mandatory verticles and extensions.
	 * 
	 * @param startupHandler
	 * @throws Exception
	 */
	public void run(Runnable startupHandler) throws Exception {

		printProductInformation();

		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MeshSpringConfiguration.class)) {
			SpringVerticleFactory.setParentContext(ctx);
			BootstrapInitializer initalizer = ctx.getBean(BootstrapInitializer.class);
			ctx.start();
			initalizer.init(options, verticleLoader);
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
		return new io.vertx.core.Starter().getVersion();
	}

	private static String infoLine(String text) {
		return "# " + StringUtils.rightPad(text, 45) + " #";
	}

	public static String getVersion() {
		Package pack = MeshImpl.class.getPackage();
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

	public MeshOptions getOptions() {
		return options;
	}

}
