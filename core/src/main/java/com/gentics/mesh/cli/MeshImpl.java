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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.vertx.core.http.*;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.VersionUtil;

import io.reactivex.Completable;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.impl.launcher.commands.VersionCommand;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

/**
 * @see Mesh
 */
public class MeshImpl implements Mesh {

	private static AtomicLong instanceCounter = new AtomicLong(0);

	private static final Logger log;
	private final MeshComponent.Builder builder;

	private MeshCustomLoader<Vertx> verticleLoader;

	private MeshOptions options;

	private CountDownLatch latch = new CountDownLatch(1);

	private MeshStatus status = MeshStatus.STARTING;

	private MeshComponent meshInternal;

	boolean shutdown = false;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		log = LoggerFactory.getLogger(MeshImpl.class);
	}

	public MeshImpl(MeshOptions options, MeshComponent.Builder builder) {
		this.builder = builder;
		long current = instanceCounter.incrementAndGet();
		if (current >= 2) {
			if (options.getClusterOptions().isEnabled()) {
				throw new RuntimeException("Clustering is currently limited to a single instance mode.");
			}
		}
		Objects.requireNonNull(options, "Please specify a valid options object.");
		this.options = options;
	}

	@Override
	public Vertx getVertx() {
		if (meshInternal == null) {
			return null;
		}
		return meshInternal.vertx();
	}

	@Override
	public io.vertx.reactivex.core.Vertx getRxVertx() {
		if (getVertx() == null) {
			return null;
		}

		return new io.vertx.reactivex.core.Vertx(getVertx());
	}

	@Override
	public Mesh run() throws Exception {
		run(true);
		return this;
	}

	@Override
	public Mesh run(boolean block) throws Exception {
		shutdown = false;
		checkSystemRequirements();

		setupKeystore(options);

		registerShutdownHook();

		// An old lock file has been detected. Normally the lock file should be removed during shutdown.
		// A old lock file means that mesh did not shutdown in a clean way. We invoke a index sync of
		// the ES index in those cases in order to ensure consistency.
		boolean forceIndexSync = hasLockFile();
		if (!forceIndexSync) {
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
		meshInternal = builder.configuration(options).mesh(this).build();
		setMeshInternal(meshInternal);
		try {
			meshInternal.boot().init(this, forceIndexSync, options, verticleLoader);
		} catch (Throwable e1) {
			log.fatal("Fatal error on Mesh init", e1);
			shutdown();
			return this;
		}
		if (options.isUpdateCheckEnabled()) {
			try {
				invokeUpdateCheck();
			} catch (Exception e) {
				if (log.isTraceEnabled()) {
					log.trace("Error while checking for updates", e);
				}
			}
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
			// The needed nashorn classfilter was removed from the JRE 15 (https://openjdk.org/jeps/372)
			getClass().getClassLoader().loadClass("org.openjdk.nashorn.api.scripting.ClassFilter");
		} catch (ClassNotFoundException e) {
			log.error(
				"The nashorn classfilter could not be found. Make sure the required dependencies are in the classpath");
			System.exit(10);
		}
	}

	/**
	 * Send a request to the update checker.
	 */
	public void invokeUpdateCheck() {
		String currentVersion = Mesh.getPlainVersion();
		log.info("Checking for updates..");

		RequestOptions requestOptions = new RequestOptions();
		requestOptions.setMethod(HttpMethod.GET);
		requestOptions.setSsl(true);
		requestOptions.setHost("getmesh.io/api/updatecheck?v=" + Mesh.getPlainVersion());
		getVertx().createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(false))
				.request(HttpMethod.GET, 443, "getmesh.io", "/api/updatecheck?v=" + Mesh.getPlainVersion(), ar -> {
					if (ar.succeeded()) {
						HttpClientRequest req = ar.result();

						MultiMap headers = req.headers();
						headers.set("content-type", "application/json");
						String hostname = getHostname();
						if (!isEmpty(hostname)) {
							headers.set("X-Hostname", hostname);
						}

						req.send(ar2 -> {
							if (ar2.succeeded()) {
								HttpClientResponse response = ar2.result();
								int code = response.statusCode();
								if (code < 200 || code >= 299) {
									log.error("Update check failed with status code {" + code + "}");
								} else {
									response.bodyHandler(bh -> {
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
							} else {
								log.info("Failed to check for updates.");
								log.debug("Reason for failed update check", ar2.cause());
							}
						});
					} else {
						log.info("Failed to check for updates.");
						log.debug("Reason for failed update check", ar.cause());
					}
				});
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
	public void shutdownAndTerminate(int code) {
		Runtime.getRuntime().exit(code);
	}

	@Override
	public void shutdown() throws Exception {
		if (shutdown) {
			log.info("Instance is already shut down...");
			return;
		}

		log.info("Mesh shutting down...");

		// status
		try {
			log.info("Setting shutdown status");
			setStatus(MeshStatus.SHUTTING_DOWN);
		} catch (Throwable t) {
			log.error("Error while setting shutdown status", t);
		}

		// plugins
		try {
			log.info("Undeploying plugins");
			meshInternal.pluginManager().stop().blockingAwait(getOptions().getPluginTimeout(), TimeUnit.SECONDS);
		} catch (Throwable t) {
			log.error("One of the plugins could not be undeployed in the allotted time.", t);
		}

		// search
		try {
			log.info("Stopping search provider");
			meshInternal.searchProvider().stop();
		} catch (Throwable t) {
			log.error("The search provider did encounter an error while stopping", t);
		}

		// vert.x
		try {
			io.vertx.reactivex.core.Vertx rxVertx = getRxVertx();
			if (rxVertx != null) {
				log.info("Stopping Vert.x");
				rxVertx.rxClose().blockingAwait();
			}
		} catch (Throwable t) {
			log.error("Error while stopping Vert.x", t);
		}

		// database
		try {
			log.info("Stopping and closing database provider");
			meshInternal.database().stop();
		} catch (Throwable t) {
			log.error("Error while stopping database", t);
		}

		try {
			log.info("Shutting database provider down");
			meshInternal.database().shutdown();
		} catch (Throwable t) {
			log.error("Error while stopping database", t);
		}

		// boot
		try {
			log.info("Clearing references");
			BootstrapInitializer boot = meshInternal.boot();
			if (boot != null) {
				boot.clearReferences();
			}
		} catch (Throwable t) {
			log.error("Error while clearing refs", t);
		}

		// liveness manager
		log.info("Stopping liveness manager");
		meshInternal.livenessManager().shutdown();

		deleteLock();
		meshInternal = null;
		log.info("Shutdown completed...");

		try {
			latch.countDown();
		} catch (Exception e) {
			log.debug("Error while releasing latch. Maybe it was already released.", e);
		}

		shutdown = true;
	}

	/**
	 * Create a new mesh lock file.
	 * 
	 * @throws IOException
	 */
	private void createLockFile() throws IOException {
		File lockFile = new File(options.getLockPath());
		File lockFolder = lockFile.getParentFile();
		if (lockFolder != null && !lockFolder.exists() && !lockFolder.mkdirs()) {
			log.error("Could not create parent folder for lockfile {" + lockFile.getAbsolutePath() + "}");
		}
		lockFile.createNewFile();
	}

	/**
	 * Check whether the mesh lock file exists.
	 * 
	 * @return
	 */
	private boolean hasLockFile() {
		return new File(options.getLockPath()).exists();
	}

	/**
	 * Delete the mesh lock file.
	 */
	private void deleteLock() {
		new File(options.getLockPath()).delete();
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

	@Override
	public Completable deployPlugin(Class<?> clazz, String id) {
		return Completable.defer(() -> {
			return meshInternal.pluginManager().deploy(clazz, id);
		});
	}

	@Override
	public Set<String> pluginIds() {
		return meshInternal.pluginManager().getPluginIds();
	}

	@Override
	public <T> T internal() {
		return (T) meshInternal;
	}

	@Override
	public <T> void setMeshInternal(T meshInternal) {
		this.meshInternal = (MeshComponent) meshInternal;
	}

}
