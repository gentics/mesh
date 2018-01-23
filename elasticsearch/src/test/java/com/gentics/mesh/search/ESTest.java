package com.gentics.mesh.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class ESTest {

	private static final Logger log = LoggerFactory.getLogger(ESTest.class);

	@Test
	@Ignore
	public void testExec() throws IOException, ZipException {

		File outputDir = new File("es");
		if (!outputDir.exists()) {
			unzip("/elasticsearch-6.1.2.zip", outputDir.getAbsolutePath());
		}

		File esDir = new File(outputDir, "elasticsearch-6.1.2");
		if (!esDir.exists()) {
			throw new FileNotFoundException("Could not find elasticsearch in {" + esDir.getAbsolutePath() + "}");
		}

		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(esDir);
		builder.command("sh", "-c", "chmod +x bin/elasticsearch && ./bin/elasticsearch");

		Process p = builder.start();

		StringBuffer sb = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = "";
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		System.out.println(sb.toString());
		// StreamGobbler streamGobbler =
		// new StreamGobbler(process.getInputStream(), System.out::println);
		// Executors.newSingleThreadExecutor().submit(streamGobbler);
		// int exitCode = process.waitFor();
		// assert exitCode == 0;
		// Process process = Runtime.getRuntime().exec(String.format("sh -c ls %s", "."));

	}

	public static void unzip(String zipClasspath, String outdir) throws FileNotFoundException, IOException, ZipException {
		InputStream ins = ESTest.class.getResourceAsStream(zipClasspath);
		if (ins != null) {
			File zipFile = new File(System.getProperty("java.io.tmpdir"), "mesh-demo.zip");
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
