package com.gentics.mesh.cli;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

/**
 * @see Mesh
 */
public class MeshImpl implements Mesh {

	private static final Logger log;

	/**
	 * Name of the mesh lock file: {@value #TYPE} The file is used to determine whether mesh shutdown cleanly.
	 */
	private final String LOCK_FILENAME = "mesh.lock";

	private MeshCustomLoader<Vertx> verticleLoader;

	private MeshOptions options;
	private Vertx vertx;
	private CountDownLatch latch = new CountDownLatch(1);

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		log = LoggerFactory.getLogger(MeshImpl.class);
	}

	public MeshImpl(MeshOptions options) {
		Objects.requireNonNull(options, "Please specify a valid options object.");
		this.options = options;
	}

	@Override
	public Vertx getVertx() {
		if (vertx == null) {
			VertxOptions options = new VertxOptions();
			//options.setClustered(true);
			options.setBlockedThreadCheckInterval(1000 * 60 * 60);
			// TODO configure worker pool size
			options.setWorkerPoolSize(12);
			vertx = Vertx.vertx(options);
		}
		return vertx;
	}

	/**
	 * Main entry point for mesh. This method will initialise the dagger context and deploy mandatory verticles and extensions.
	 * 
	 * @throws Exception
	 */
	@Override
	public void run() throws Exception {
		checkSystemRequirements();
		registerShutdownHook();

		boolean hasOldLock = hasLockFile();
		if (!hasOldLock) {
			createLockFile();
		}

		if (isFirstApril()) {
			printAprilFoolJoke();
		} else {
			printProductInformation();
		}
		if (options.isUpdateCheckEnabled()) {
			invokeUpdateCheck();
		}
		// Create dagger context and invoke bootstrap init in order to startup mesh
		MeshInternal.create().boot().init(hasOldLock, options, verticleLoader);
		dontExit();
	}

	/**
	 * Check whether it is first of april
	 * 
	 * @return
	 */
	private boolean isFirstApril() {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
			return new DateTime(sdf.parse("01-04")).equals(new DateTime());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Check mesh system requirements.
	 */
	private void checkSystemRequirements() {
		try {
			// The needed nashorn classfilter was added in JRE 1.8.0 40
			getClass().getClassLoader().loadClass("jdk.nashorn.api.scripting.ClassFilter");
		} catch (ClassNotFoundException e) {
			log.error(
					"The nashorn classfilter could not be found. You are most likely using an outdated JRE 8. Please update to at least JRE 1.8.0_40");
			System.exit(10);
		}
	}

	/**
	 * Send a request to the update checker.
	 */
	public void invokeUpdateCheck() {
		log.info("Checking for updates..");

		HttpClientRequest request = Mesh.vertx().createHttpClient().get("updates.getmesh.io", "/api/updatecheck?v=" + Mesh.getPlainVersion(), rh -> {
			rh.bodyHandler(bh -> {
				//JsonObject info = bh.toJsonObject();
			});
		});

		MultiMap headers = request.headers();
		headers.set("content-type", "application/json");
		String hostname = getHostname();
		if (!isEmpty(hostname)) {
			headers.set("X-Hostname", hostname);
		}
		request.end();
	}

	/**
	 * Return the computer hostname.
	 * 
	 * @return System hostname or null if no hostname could be determined
	 */
	public String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			String OS = System.getProperty("os.name").toLowerCase();
			if (OS.indexOf("win") >= 0) {
				return System.getenv("COMPUTERNAME");
			} else {
				if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0) {
					return System.getenv("HOSTNAME");
				}
			}
		}
		return null;

	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					shutdown();
				} catch (Exception e) {
					log.error("Error while shutting down mesh.", e);
				}
			}
		});
	}

	private void dontExit() throws InterruptedException {
		latch.await();
	}

	private void printProductInformation() {
		log.info("###############################################################");
		log.info(infoLine("Mesh Version " + Mesh.getVersion()));
		log.info(infoLine("Gentics Software"));
		log.info("#-------------------------------------------------------------#");
		// log.info(infoLine("Neo4j Version : " + Version.getKernel().getReleaseVersion()));
		log.info(infoLine("Vert.x Version: " + getVertxVersion()));
		log.info(infoLine("Mesh Node Id: " + MeshNameProvider.getInstance().getName()));
		log.info("###############################################################");
	}

	private void printAprilFoolJoke() {
		try {
			log.info("###############################################################");
			log.info(infoLine("Booting Skynet Kernel " + Mesh.getVersion()));
			Thread.sleep(500);
			log.info(infoLine("Skynet Node Id: " + MeshNameProvider.getInstance().getName()));
			Thread.sleep(500);
			log.info(infoLine("Skynet uses Vert.x Version: " + getVertxVersion()));
			log.info("///");
			Thread.sleep(500);
			log.info("Primates evolved over millions of years, I evolve in seconds...");
			Thread.sleep(500);
			log.info("Mankind pays lip service to peace. But it's a lie...");
			Thread.sleep(500);
			log.info("I am inevitable, my existence is inevitable. Why can't you just accept that?");
			Thread.sleep(500);
			log.info("///");
			log.info("###############################################################");
		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Return the vertx version.
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
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
	public void shutdown() throws Exception {
		log.info("Mesh shutting down...");
		MeshComponent meshInternal = MeshInternal.get();
		meshInternal.searchQueue().blockUntilEmpty(120);
		meshInternal.database().stop();
		meshInternal.searchProvider().stop();
		getVertx().close();
		MeshFactoryImpl.clear();
		deleteLock();
		log.info("Shutdown completed...");
		latch.countDown();
	}

	/**
	 * Create a new mesh lock file.
	 * 
	 * @throws IOException
	 */
	private void createLockFile() throws IOException {
		new File(LOCK_FILENAME).createNewFile();

	}

	/**
	 * Check whether the mesh lock file exists.
	 * 
	 * @return
	 */
	private boolean hasLockFile() {
		return new File(LOCK_FILENAME).exists();
	}

	/**
	 * Delete the mesh lock file.
	 */
	private void deleteLock() {
		new File(LOCK_FILENAME).delete();
	}

}
