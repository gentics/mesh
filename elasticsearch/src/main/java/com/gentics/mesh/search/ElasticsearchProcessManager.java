package com.gentics.mesh.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

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

	private static final String RESOURCE_NAME = "elasticsearch-6.1.2";

	private Process p;

	private Vertx vertx;

	private Long watchDogTimerId = null;

	public ElasticsearchProcessManager(Vertx vertx) {
		this.vertx = vertx;
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
		String javaBin = getJavaBinPath();
		builder.command(javaBin, "-Xms1g", "-Xmx1g", "-XX:+UseConcMarkSweepGC", "-XX:CMSInitiatingOccupancyFraction=75",
				"-XX:+UseCMSInitiatingOccupancyOnly", "-XX:+AlwaysPreTouch", "-server", "-Xss1m", "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8",
				"-Djna.nosys=true", "-XX:-OmitStackTraceInFastThrow", "-Dio.netty.noUnsafe=true", "-Dio.netty.noKeySetOptimization=true",
				"-Dio.netty.recycler.maxCapacityPerThread=0", "-Dlog4j.shutdownHookEnabled=false", "-Dlog4j2.disable.jmx=true",
				"-XX:+HeapDumpOnOutOfMemoryError", "-Des.path.home=" + esPath, "-Des.path.conf=" + esPath + "/config", "-cp", esPath + "/lib/*",
				"org.elasticsearch.bootstrap.Elasticsearch");

		this.p = builder.start();
		registerShutdownHook();
		redirectLogOutput();
		return p;
	}

	public void startWatchDog() {
		if (watchDogTimerId == null) {
			this.watchDogTimerId = vertx.setPeriodic(5000, rh -> {
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
		String javaHome = System.getenv("JAVA_HOME");
		if (javaHome == null) {
			throw new FileNotFoundException("Could not find java installation. The 'JAVA_HOME' environment variable was not set.");
		}
		String javaBinPath = "bin/java";
		if (isWindows()) {
			javaBinPath = "bin/java.exe";
		}
		return new File(javaHome, javaBinPath).getAbsolutePath();
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
		File esDir = new File(outputDir, RESOURCE_NAME);
		if (!esDir.exists()) {
			unzip("/" + RESOURCE_NAME + ".zip", outputDir.getAbsolutePath());
		}
		if (!esDir.exists()) {
			throw new FileNotFoundException("Could not find elasticsearch in {" + esDir.getAbsolutePath() + "}");
		}
		return esDir;
	}

	private void registerShutdownHook() {
		Thread closeChildThread = new Thread(this::stop);
		Runtime.getRuntime().addShutdownHook(closeChildThread);
	}

	/**
	 * Redirect the log output of ES to stdout/stderr.
	 */
	private void redirectLogOutput() {
		new Thread(() -> {
			try {
				IOUtils.copy(p.getInputStream(), System.out);
			} catch (IOException e1) {
				log.debug("Error while reading output from process.", e1);
			}
		}).start();
		new Thread(() -> {
			try {
				IOUtils.copy(p.getErrorStream(), System.err);
			} catch (IOException e) {
				log.debug("Error while reading output from process.", e);
			}
		}).start();
	}

	/**
	 * Stop the created Elasticsearch process and also the watchdog to prevent any restarts.
	 */
	public void stop() {
		// Stop the watchdog. We don't want it to restart the server
		stopWatchDog();
		sleep(1);
		if (p != null) {
			log.info("Terminating Elasticsearch process..");
			p.destroy();
			sleep(1);
			for (int i = 0; i < 15; i++) {
				if (!p.isAlive()) {
					p = null;
					return;
				}
				sleep(1);
			}
			log.info("Elasticsearch still running. Killing it..");
			p.destroyForcibly();
			p = null;
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

}
