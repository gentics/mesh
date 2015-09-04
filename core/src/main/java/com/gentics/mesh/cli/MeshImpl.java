package com.gentics.mesh.cli;

import java.util.Objects;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class MeshImpl implements Mesh {

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

	public static Mesh instance() {
		if (instance == null) {
			throw new RuntimeException("Mesh instance not yet initialized.");
		}
		return instance;
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

	public static MeshImpl create(MeshOptions options) {
		if (instance == null) {
			instance = new MeshImpl(options);
		}
		return instance;
	}

	public static Mesh create(MeshOptions options, Vertx vertx) {
		if (instance == null) {
			instance = new MeshImpl(options, vertx);
		}
		return instance;
	}

	@Override
	public Vertx getVertx() {
		if (vertx == null) {
			VertxOptions options = new VertxOptions();
			options.setBlockedThreadCheckInterval(1000 * 60 * 60);
			//TODO configure worker pool size
			options.setWorkerPoolSize(16);
			vertx = Vertx.vertx(options);
		}
		return vertx;
	}

	/**
	 * Main entry point for mesh. This method will initialize the spring context and deploy mandatory verticles and extensions.
	 * 
	 * @throws Exception
	 */
	@Override
	public void run() throws Exception {
		registerShutdownHook();

		printProductInformation();

		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MeshSpringConfiguration.class)) {
			setParentContext(ctx);
			BootstrapInitializer initalizer = ctx.getBean(BootstrapInitializer.class);
			ctx.start();
			initalizer.init(options, verticleLoader);
			ctx.registerShutdownHook();

			dontExit();

		}
	}

	/**
	 * This method injects the spring context into the spring verticle factory. This method should be replaced by SpringVerticleFactory.setParentContext(ctx);
	 * as soon as version 2.0.1 is released.
	 * 
	 * @param ctx
	 */
	@Deprecated
	private void setParentContext(AnnotationConfigApplicationContext ctx) {
		java.lang.reflect.Field field;
		try {
			field = SpringVerticleFactory.class.getDeclaredField("parentContext");
			field.setAccessible(true);
			field.set(null, ctx);
		} catch (Exception e) {
			log.error("Could not set spring context", e);
			System.exit(10);
		}

	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown();
			}
		});
	}

	/**
	 * Handle command line arguments.
	 * 
	 * @param args
	 * @throws ParseException
	 */
	@Override
	public void handleArguments(String[] args) throws ParseException {
		// TODO Not yet implemented
	}

	private void dontExit() {
		while (true) {
			//TODO use unsafe park instead
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void printProductInformation() {
		log.info("#####################################################");
		log.info(infoLine("Mesh Version " + Mesh.getVersion()));
		log.info(infoLine("Gentics Software GmbH"));
		log.info("#---------------------------------------------------#");
		//log.info(infoLine("Neo4j Version : " + Version.getKernel().getReleaseVersion()));
		log.info(infoLine("Vert.x Version: " + getVertxVersion()));
		log.info(infoLine("Name:" + MeshNameProvider.getInstance().getName()));
		log.info("#####################################################");
	}

	private String getVertxVersion() {
		return new io.vertx.core.Starter().getVersion();
	}

	private static String infoLine(String text) {
		return "# " + StringUtils.rightPad(text, 49) + " #";
	}

	/**
	 * Set a custom verticle loader that will be invoked once all major components have been initialized.
	 * 
	 * @param verticleLoader
	 */
	@Override
	public void setCustomLoader(MeshCustomLoader<Vertx> verticleLoader) {
		this.verticleLoader = verticleLoader;
	}

	@Override
	public MeshOptions getOptions() {
		return options;
	}

	@Override
	public void shutdown() {
		log.info("Mesh shutting down...");
		//Orientdb has a dedicated shutdown hook
		MeshSpringConfiguration.getMeshSpringConfiguration().searchProvider().stop();
		getVertx().close();
	}

}
