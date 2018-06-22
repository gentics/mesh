package com.gentics.mesh.jmh;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractJMHRunner {

	public static final Logger log = LoggerFactory.getLogger(AbstractJMHRunner.class);

	public static File OUTPUT_DIR = new File("target/results");

	public static ChainedOptionsBuilder create(String[] args) throws RunnerException, IOException {
		if (args.length != 1) {
			throw new RuntimeException("Invalid arguments. Only the name of the benchmark output file can be specified.");
		}
		String name = args[0];
		if (!OUTPUT_DIR.exists()) {
			if (!OUTPUT_DIR.mkdirs()) {
				throw new RuntimeException("Could not create output dir {" + OUTPUT_DIR.getAbsolutePath() + "}");
			}
		}

		return new OptionsBuilder()
			.forks(1)
			.warmupIterations(2)
			.measurementIterations(2)
			.resultFormat(ResultFormatType.JSON)
			.result(new File(OUTPUT_DIR, name + ".json").getAbsolutePath())
			.verbosity(VerboseMode.EXTRA);

	}

	public static void run(Options opt, boolean generateProvidedFile) throws RunnerException, IOException {
		new Runner(opt).run();

		if (generateProvidedFile) {
			generateProvidedJson();
		}
	}

	/**
	 * Generate a provided.js file which can be used in conjunction with JMH Visualizer.
	 * 
	 * @throws IOException
	 */
	private static void generateProvidedJson() throws IOException {

		StringBuilder builder = new StringBuilder();

		List<String> names = new ArrayList<>();
		builder.append("var providedBenchmarkStore = {\n");
		Set<String> contents = Files.list(OUTPUT_DIR.toPath())
			.filter(Files::isRegularFile)
			.filter(f -> f.getFileName().toString().endsWith(".json"))
			.sorted((a, b) -> {
				String aVersion = a.getFileName().toString();
				aVersion = aVersion.replaceAll(".json", "");
				String bVersion = b.getFileName().toString();
				bVersion = bVersion.replaceAll(".json", "");
				return compareVersions(aVersion, bVersion);
			})
			.map(file -> {
				String name = file.getFileName().toString();
				name = name.replaceAll(".json", "");
				name = "'" + name + "'";
				names.add(name);
				log.info("Handling result file {" + file + "}");
				try {
					String content = new String(Files.readAllBytes(file));
					return name + ":" + content;
				} catch (IOException e) {
					throw new RuntimeException("Could not read file {" + file + "}");
				}
			}).collect(Collectors.toSet());

		String joinedJson = String.join(",", contents);
		builder.append(joinedJson);
		builder.append("};\n");

		String joined = String.join(",", names);
		builder.append("var providedBenchmarks = [" + joined + "];\n");

		String fileContent = builder.toString();
		log.debug("Generated provided.json:\n" + fileContent);
		Files.write(new File(OUTPUT_DIR, "provided.js").toPath(), fileContent.getBytes());
	}

	/**
	 * Compare both versions and return the delta of the versions.
	 * 
	 * @param version1
	 * @param version2
	 * @return 0, when both versions are equal, &lt;0 when the second version is larger, &gt;0 when the second version is smaller
	 */
	public static int compareVersions(String version1, String version2) {

		String[] levels1 = version1.split("\\.");
		String[] levels2 = version2.split("\\.");

		int length = Math.max(levels1.length, levels2.length);
		for (int i = 0; i < length; i++) {
			Integer v1 = i < levels1.length ? Integer.parseInt(levels1[i]) : 0;
			Integer v2 = i < levels2.length ? Integer.parseInt(levels2[i]) : 0;
			int compare = v1.compareTo(v2);
			if (compare != 0) {
				return compare;
			}
		}

		return 0;
	}

}