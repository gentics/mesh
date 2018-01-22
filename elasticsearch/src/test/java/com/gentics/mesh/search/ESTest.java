package com.gentics.mesh.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

public class ESTest {

	@Test
	public void testExec() throws IOException {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command("sh", "-c", "bin/elasticsearch");

		builder.directory(new File("es"));
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
}
