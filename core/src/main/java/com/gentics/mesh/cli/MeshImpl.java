package com.gentics.mesh.cli;

import static com.gentics.mesh.MeshEnv.MESH_CONF_FILENAME;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.plugin.PluginManager;
import com.gentics.mesh.util.VersionUtil;

import io.vertx.core.MultiMap;
import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.impl.launcher.commands.VersionCommand;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.dropwizard.MetricsService;

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

	private io.vertx.reactivex.core.Vertx rxVertx;

	private CountDownLatch latch = new CountDownLatch(1);

	private MeshStatus status = MeshStatus.STARTING;

	private MetricsService metricsService;

	private static PluginManager pluginManager = ServiceHelper.loadFactory(PluginManager.class);

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
		return vertx;
	}

	@Override
	public io.vertx.reactivex.core.Vertx getRxVertx() {
		if (vertx == null) {
			return null;
		}
		if (rxVertx == null) {
			rxVertx = new io.vertx.reactivex.core.Vertx(vertx);
		}
		return rxVertx;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public Mesh run() throws Exception {
		run(true);
		return this;
	}

	@Override
	public Mesh run(boolean block) throws Exception {
		checkSystemRequirements();

		setupKeystore(options);

		registerShutdownHook();

		// An old lock file has been detected. Normally the lock file should be removed during shutdown.
		// A old lock file means that mesh did not shutdown in a clean way. We invoke a full reindex of
		// the ES index in those cases in order to ensure consistency.
		boolean forceReindex = hasLockFile();
		if (!forceReindex) {
			createLockFile();
		}

		// // Also trigger the reindex if the index folder could not be found.
		// String indexDir = options.getSearchOptions().getDirectory();
		// if (indexDir != null) {
		// File folder = new File(indexDir);
		// if (!folder.exists() || folder.listFiles().length == 0) {
		// forceReindex = true;
		// }
		// }

		if (isFirstApril()) {
			printAprilFoolJoke();
		} else {
			printProductInformation();
		}
		// Create dagger context and invoke bootstrap init in order to startup mesh
		try {
			MeshInternal.create(options).boot().init(this, forceReindex, options, verticleLoader);

			if (options.isUpdateCheckEnabled()) {
				try {
					invokeUpdateCheck();
				} catch (Exception e) {
					// Ignored
				}
			}
		} catch (Exception e) {
			log.error("Error while starting mesh", e);
			shutdown();
		}
		setStatus(MeshStatus.READY);
		if (block) {
			dontExit();
		}
		return this;
	}

	private void setupKeystore(MeshOptions options) throws Exception {
		String keyStorePath = options.getAuthenticationOptions().getKeystorePath();
		File keystoreFile = new File(keyStorePath);
		// Copy the demo keystore file to the destination
		if (!keystoreFile.exists()) {
			log.info("Could not find keystore {" + keyStorePath + "}. Creating one for you..");
			if (keystoreFile.getParentFile() == null) {
				log.debug("No parent directory for keystore found. Trying to create the keystore in the mesh root directory.");
			} else {
				log.debug("Ensure the keystore parent directory exists " + keyStorePath);
				keystoreFile.getParentFile().mkdirs();
			}
			KeyStoreHelper.gen(keyStorePath, options.getAuthenticationOptions().getKeystorePassword());
			log.info("Keystore {" + keyStorePath + "} created. The keystore password is listed in your {" + MESH_CONF_FILENAME + "} file.");
		}
	}

	/**
	 * Check whether it is first of april
	 * 
	 * @return
	 */
	private boolean isFirstApril() {
		LocalDate now = LocalDate.now();
		return now.getDayOfMonth() == 1 && now.getMonth() == Month.APRIL;
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
		String currentVersion = Mesh.getPlainVersion();
		log.info("Checking for updates..");
		HttpClientRequest request = Mesh.vertx().createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(false)).get(443, "getmesh.io",
			"/api/updatecheck?v=" + Mesh.getPlainVersion(), rh -> {
				int code = rh.statusCode();
				if (code < 200 || code >= 299) {
					log.error("Update check failed with status code {" + code + "}");
				} else {
					rh.bodyHandler(bh -> {
						JsonObject info = bh.toJsonObject();
						String latestVersion = info.getString("latest");

						if (currentVersion.contains("-SNAPSHOT")) {
							log.warn("You are using a SNAPSHOT version {" + currentVersion
								+ "}. This is potentially dangerous because this version has never been officially released.");
							log.info("The latest version of Gentics Mesh is {" + latestVersion + "}");
						} else {
							int result = VersionUtil.compareVersions(latestVersion, currentVersion);
							if (result == 0) {
								log.info("Great! You are using the latest version");
							} else if (result > 0) {
								log.warn("Your Gentics Mesh version is outdated. You are using {" + currentVersion + "} but version {"
									+ latestVersion + "} is available.");
							}
						}
					});
				}
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
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				shutdown();
			} catch (Exception e) {
				// Use system out since logging system may have been shutdown
				System.err.println("Error while shutting down mesh.");
				e.printStackTrace();
			}
		}));
	}

	@Override
	public void dontExit() throws InterruptedException {
		latch.await();
	}

	private void printProductInformation() {
		log.info("###############################################################");
		log.info(infoLine("Mesh Version " + MeshVersion.getBuildInfo()));
		log.info(infoLine("Gentics Software"));
		log.info("#-------------------------------------------------------------#");
		// log.info(infoLine("Neo4j Version : " + Version.getKernel().getReleaseVersion()));
		log.info(infoLine("Vert.x Version: " + getVertxVersion()));
		if (getOptions().getClusterOptions() != null && getOptions().getClusterOptions().isEnabled()) {
			log.info(infoLine("Cluster Name: " + getOptions().getClusterOptions().getClusterName()));
		}
		log.info(infoLine("Mesh Node Name: " + getOptions().getNodeName()));
		log.info("###############################################################");
	}

	private void printAprilFoolJoke() {
		try {
			log.info("###############################################################");
			log.info(infoLine("Booting Skynet Kernel " + MeshVersion.getBuildInfo()));
			Thread.sleep(500);
			if (getOptions().getClusterOptions() != null && getOptions().getClusterOptions().isEnabled()) {
				log.info(infoLine("Skynet Global Name: " + getOptions().getClusterOptions().getClusterName()));
				Thread.sleep(500);
			}
			log.info(infoLine("Skynet Node Name: " + getOptions().getNodeName()));
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
	private String getVertxVersion() {
		return VersionCommand.getVersion();
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
		setStatus(MeshStatus.SHUTTING_DOWN);
		try {
			pluginManager.stop().blockingAwait(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("One of the plugins could not be undeployed in the allotted time.", e);
		}
		MeshComponent meshInternal = MeshInternal.get();
		meshInternal.database().stop();
		try {
			meshInternal.searchProvider().stop();
		} catch (Exception e) {
			log.error("The search provider did encounter an error while stopping", e);
		}
		Vertx vertx = getVertx();
		if (vertx != null) {
			vertx.close();
		}
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

	@Override
	public MeshStatus getStatus() {
		return status;
	}

	@Override
	public Mesh setStatus(MeshStatus status) {
		this.status = status;
		return this;
	}

	public void setMetricsService(MetricsService metricsService) {
		this.metricsService = metricsService;
	}

	@Override
	public MetricsService metrics() {
		Objects.requireNonNull(metricsService, "The metrics service can only be used once Gentics Mesh has been setup.");
		return metricsService;
	}

}
