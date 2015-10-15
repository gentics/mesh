package com.gentics.mesh.cli;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class MeshImpl implements Mesh {

	private static final Logger log;

	private MeshCustomLoader<Vertx> verticleLoader;

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

	@Override
	public Vertx getVertx() {
		if (vertx == null) {
			VertxOptions options = new VertxOptions();
			options.setBlockedThreadCheckInterval(1000 * 60 * 60);
			// TODO configure worker pool size
			options.setWorkerPoolSize(12);
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

		// Start the spring context
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
			throw new RuntimeException("Spring context setup failed");
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

	private void dontExit() {
		while (true) {
			// TODO use unsafe park instead
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void printProductInformation() {
		log.info("###############################################################");
		log.info(infoLine("Mesh Version " + Mesh.getVersion()));
		log.info(infoLine("Gentics Software GmbH"));
		log.info("#-------------------------------------------------------------#");
		// log.info(infoLine("Neo4j Version : " + Version.getKernel().getReleaseVersion()));
		log.info(infoLine("Vert.x Version: " + getVertxVersion()));
		log.info(infoLine("Name: " + MeshNameProvider.getInstance().getName()));
		log.info("###############################################################");
	}

	private String getVertxVersion() {
		return new io.vertx.core.Starter().getVersion();
	}

	private static String infoLine(String text) {
		return "# " + StringUtils.rightPad(text, 59) + " #";
	}

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
		MeshSpringConfiguration.getInstance().database().stop();
		MeshSpringConfiguration.getInstance().searchProvider().stop();
		getVertx().close();
		MeshFactoryImpl.clear();
	}

}
