package com.gentics.mesh.jmh;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.jmh.model.JMHCollection;
import com.gentics.mesh.jmh.model.JMHResult;
import com.gentics.mesh.json.JsonUtil;

public final class JMHUtil {

	public static final Logger log = LoggerFactory.getLogger(JMHUtil.class);

	private JMHUtil() {

	}

	/**
	 * Generate a provided.js file which can be used in conjunction with JMH Visualizer.
	 * 
	 * @param outputDir
	 * @throws IOException
	 */
	public static void generateProvidedJson(Path outputDir) throws IOException {

		StringBuilder builder = new StringBuilder();

		List<String> names = new ArrayList<>();
		builder.append("var providedBenchmarkStore = {\n");
		Set<String> contents = Files.list(outputDir)
			.filter(Files::isRegularFile)
			.filter(f -> f.getFileName().toString().endsWith(".json"))
			.map(f -> {
				try {
					log.info("Handling result file {" + f + "}");
					String content = new String(Files.readAllBytes(f));
					return JsonUtil.readValue(content, JMHCollection.class);
				} catch (IOException e) {
					throw new RuntimeException("Could not read file {" + f + "}");
				}
			})
			.sorted((a, b) -> {
				String aVersion = a.getVersion();
				String bVersion = b.getVersion();
				return compareVersions(aVersion, bVersion);
			})
			.map(result -> {
				return result.getVersion() + ":" + result.toJson();
			})
			.collect(Collectors.toSet());

		String joinedJson = String.join(",", contents);
		builder.append(joinedJson);
		builder.append("};\n");

		String joined = String.join(",", names);
		builder.append("var providedBenchmarks = [" + joined + "];\n");

		String fileContent = builder.toString();
		log.debug("Generated provided.json:\n" + fileContent);
		Files.write(new File(outputDir.toFile(), "provided.js").toPath(), fileContent.getBytes());
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
			try {
				Integer v1 = i < levels1.length ? Integer.parseInt(levels1[i]) : 0;
				Integer v2 = i < levels2.length ? Integer.parseInt(levels2[i]) : 0;
				int compare = v1.compareTo(v2);
				if (compare != 0) {
					return compare;
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return -1;
			}
		}

		return 0;
	}

}
