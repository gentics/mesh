package com.gentics.mesh.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * This class is used to manage the forked Elasticsearch process.
 */
public final class ElasticsearchProcessManager {

	private static final Logger log = LoggerFactory.getLogger(ElasticsearchProcessManager.class);

	/**
	 * Name of the ES installation archive resource.
	 */
	private static final String RESOURCE_NAME = "elasticsearch-6.1.2";

	/**
	 * Name of the folder in which the ES installation is placed.
	 */
	private static final String OUTPUT_FOLDER_NAME = "elasticsearch";

	/**
	 * Flag that is used to record and debounce stop/restart calls.
	 */
	private static AtomicBoolean stopCalled = new AtomicBoolean(false);

	/**
	 * Check in the interval for the embedded process.
	 */
	private static final int WATCH_DOG_INTERVAL = 10;

	private Process p;

	private Vertx vertx;

	private Long watchDogTimerId = null;

	private int watchDogInterval = WATCH_DOG_INTERVAL;

	private String xmx = "1g";

	private ElasticSearchOptions options;

	public ElasticsearchProcessManager(Vertx vertx, ElasticSearchOptions options) {
		this.vertx = vertx;
		this.options = options;
	}

	/**
	 * Return the process watchdog interval.
	 * 
	 * @return
	 */
	public int getWatchDogInterval() {
		return watchDogInterval;
	}

	/**
	 * Set the interval the watchdog is checking the process.
	 * 
	 * @param intervalInSeconds
	 */
	public void setWatchDogInterval(int intervalInSeconds) {
		this.watchDogInterval = intervalInSeconds;
	}

	/**
	 * Return the maximum configured memory usage.
	 * 
	 * @return
	 */
	public String getXmx() {
		return xmx;
	}

	/**
	 * Set the maximum memory usage for the process.
	 * 
	 * @param xmx
	 */
	public void setXmx(String xmx) {
		this.xmx = xmx;
	}

	/**
	 * Prepare the Elasticsearch installation and start the Elasticsearch process.
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ZipException
	 */
	public Process start() throws FileNotFoundException, IOException, ZipException {
		File esDir = prepareESDirectory();

		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(esDir);
		String esPath = esDir.getAbsolutePath();

		List<String> allArgs = new ArrayList<>();
		allArgs.add(getJavaBinPath());

		// Add the configured arguments
		String args = getOptions().getEmbeddedArguments();
		if (args != null) {
			allArgs.addAll(Arrays.asList(args.split(" ")));
		}

		// Add the fixed classpath arguments
		allArgs.addAll(Arrays.asList("-Des.path.home=" + esPath, "-Des.path.conf=" + esPath + "/config", "-cp", esPath + "/lib/*",
			"org.elasticsearch.bootstrap.Elasticsearch"));
		builder.command(allArgs.stream().toArray(String[]::new));

		this.p = builder.start();
		// Mark the process as being started.
		stopCalled.set(false);
		registerShutdownHook();
		redirectLogOutput();
		return p;
	}

	public void startWatchDog() {
		if (watchDogTimerId == null) {
			log.info("Starting watchdog for Elasticsearch process");
			this.watchDogTimerId = vertx.setPeriodic(watchDogInterval * 1000, rh -> {
				if (p != null) {
					if (!p.isAlive()) {
						log.info("Detected stopped server. Restarting..");
						try {
							start();
						} catch (Exception e) {
							log.error("Error while starting the server via the watchdog.", e);
						}
					}
				}
			});
		}
	}

	/**
	 * Stop the watchdog timer which is checking the process status.
	 */
	public void stopWatchDog() {
		if (watchDogTimerId != null) {
			vertx.cancelTimer(watchDogTimerId);
			watchDogTimerId = null;
		}
	}

	/**
	 * Locate and return the path to the java executable.
	 * 
	 * @return
	 * @throws FileNotFoundException
	 */
	private String getJavaBinPath() throws FileNotFoundException {
		boolean isWindows = isWindows();
		String javaHome = System.getenv("JAVA_HOME");
		if (javaHome == null) {
			javaHome = System.getProperty("java.home");
		}
		if (isWindows && javaHome == null) {
			throw new FileNotFoundException("Could not find java installation. The 'JAVA_HOME' environment variable was not set.");
		}
		if (javaHome != null) {
			String javaBinPath = "bin/java";
			if (isWindows()) {
				javaBinPath = "bin/java.exe";
			}
			File javaBin = new File(javaHome, javaBinPath);
			if (!javaBin.exists()) {
				throw new FileNotFoundException(
					"Could not find java executable using JAVA_HOME {" + javaHome + "} - Was looking in {" + javaBin.getAbsolutePath() + "}");
			}
			return javaBin.getAbsolutePath();
		} else {
			return "java";
		}
	}

	/**
	 * Extract the Elasticsearch ZIP file and return the file to the folder.
	 * 
	 * @return
	 * @throws IOException
	 * @throws ZipException
	 */
	private File prepareESDirectory() throws IOException, ZipException {
		File outputDir = new File(".");
		File esDir = new File(outputDir, OUTPUT_FOLDER_NAME);
		File esBin = new File(esDir, "bin");
		if (!esBin.exists()) {
			unzip("/" + RESOURCE_NAME + ".zip", outputDir.getAbsolutePath());
		}
		if (!esDir.exists()) {
			throw new FileNotFoundException("Could not find elasticsearch in {" + esDir.getAbsolutePath() + "}");
		}
		return esDir;
	}

	/**
	 * Register the stop call on JVM shutdown.
	 */
	private void registerShutdownHook() {
		Thread closeChildThread = new Thread(this::stop);
		Runtime.getRuntime().addShutdownHook(closeChildThread);
	}

	/**
	 * Redirect the log output of ES to stdout/stderr.
	 */
	private void redirectLogOutput() {
		new Thread(() -> {
			try (InputStream ins = p.getInputStream()) {
				IOUtils.copy(ins, System.out);
			} catch (IOException e1) {
				log.debug("Error while reading output from process.", e1);
			}
		}).start();
		new Thread(() -> {
			try (InputStream ins = p.getErrorStream()) {
				IOUtils.copy(ins, System.err);
			} catch (IOException e) {
				log.debug("Error while reading output from process.", e);
			}
		}).start();
	}

	/**
	 * Stop the created Elasticsearch process and also the watchdog to prevent any restarts.
	 */
	public void stop() {
		boolean wasAlreadyCalled = stopCalled.getAndSet(true);
		if (wasAlreadyCalled) {
			// Don't handle stop twice
			return;
		}

		// Stop the watchdog. We don't want it to restart the server
		stopWatchDog();
		sleep(1);
		if (p != null) {
			log.info("Terminating Elasticsearch process..");
			p.destroy();
			sleep(1);
			for (int i = 0; i < 15; i++) {
				if (p == null || !p.isAlive()) {
					p = null;
					return;
				}
				sleep(1);
			}
			log.info("Elasticsearch still running. Killing it..");
			if (p != null) {
				p.destroyForcibly();
				p = null;
			}
		}
	}

	private static void sleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
		}
	}

	private static boolean isWindows() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("windows");
	}

	public void unzip(String zipClasspath, String outdir) throws FileNotFoundException, IOException, ZipException {
		InputStream ins = ElasticsearchProcessManager.class.getResourceAsStream(zipClasspath);
		if (ins != null) {
			// Write the classpath resource to a file so that we can extract it later
			File zipFile = new File(System.getProperty("java.io.tmpdir"), "elasticsearch.zip");
			if (zipFile.exists()) {
				zipFile.delete();
			}
			IOUtils.copy(ins, new FileOutputStream(zipFile));
			ZipFile zip = new ZipFile(zipFile);
			zip.extractAll(outdir);
			zipFile.delete();
		} else {
			log.error("The mesh-demo.zip file could not be found within the classpath {" + zipClasspath + "}");
		}
	}

	public Process getProcess() {
		return p;
	}

	public ElasticSearchOptions getOptions() {
		return options;
	}

}
