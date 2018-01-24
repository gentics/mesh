package com.gentics.mesh.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * This class is used to manage the forked Elasticsearch process.
 */
public final class ElasticsearchBundleManager {

	private static final Logger log = LoggerFactory.getLogger(ElasticsearchBundleManager.class);

	private static final String RESOURCE_NAME = "elasticsearch-6.1.2";

	public static void start() throws FileNotFoundException, IOException, ZipException {
		File outputDir = new File("es");
		if (!outputDir.exists()) {
			unzip("/" + RESOURCE_NAME + ".zip", outputDir.getAbsolutePath());
		}

		File esDir = new File(outputDir, RESOURCE_NAME);
		if (!esDir.exists()) {
			throw new FileNotFoundException("Could not find elasticsearch in {" + esDir.getAbsolutePath() + "}");
		}

		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(esDir);

		String esPath = esDir.getAbsolutePath();
		String javaHome = System.getenv("JAVA_HOME");
		String javaBin = new File(javaHome, "bin/java").getAbsolutePath();

		builder.command(javaBin, "-Xms1g", "-Xmx1g", "-XX:+UseConcMarkSweepGC", "-XX:CMSInitiatingOccupancyFraction=75",
				"-XX:+UseCMSInitiatingOccupancyOnly", "-XX:+AlwaysPreTouch", "-server", "-Xss1m", "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8",
				"-Djna.nosys=true", "-XX:-OmitStackTraceInFastThrow", "-Dio.netty.noUnsafe=true", "-Dio.netty.noKeySetOptimization=true",
				"-Dio.netty.recycler.maxCapacityPerThread=0", "-Dlog4j.shutdownHookEnabled=false", "-Dlog4j2.disable.jmx=true",
				"-XX:+HeapDumpOnOutOfMemoryError", "-Des.path.home=" + esPath, "-Des.path.conf=" + esPath + "/config", "-cp", esPath + "/lib/*",
				"org.elasticsearch.bootstrap.Elasticsearch");

		Process p = builder.start();
		Thread closeChildThread = new Thread() {
			public void run() {
				p.destroy();
			}
		};
		Runtime.getRuntime().addShutdownHook(closeChildThread);

		StringBuffer sb = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line = "";
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		System.out.println(sb.toString());

	}

	public static void unzip(String zipClasspath, String outdir) throws FileNotFoundException, IOException, ZipException {
		InputStream ins = ESTest.class.getResourceAsStream(zipClasspath);
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

}
