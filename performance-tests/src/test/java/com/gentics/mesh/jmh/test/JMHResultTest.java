package com.gentics.mesh.jmh.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.gentics.mesh.jmh.JMHUtil;
import com.gentics.mesh.jmh.model.JMHCollection;
import com.gentics.mesh.jmh.model.JMHMetric;
import com.gentics.mesh.jmh.model.JMHResult;

public class JMHResultTest {

	@Test
	public void generateJsonTest() throws IOException {
		Path path = Paths.get("target/results");
		for (int i = 0; i < 10; i++) {
			String version = "1.0." + i;
			JMHCollection collection = new JMHCollection();

			for (String group : Arrays.asList("user", "node")) {
				List<String> benchmarks = Arrays.asList("read", "create", "update");
				for (String benchmark : benchmarks) {
					JMHResult result = new JMHResult();
					result.setVersion(version);
					result.setBenchmark("com.gentics.mesh.test." + group + "." + benchmark);
					JMHMetric metric = result.getPrimaryMetric();
					metric.setRawData(Math.random(), Math.random());
					metric.setScore(Math.random());

					System.out.println(result.toJson());
					collection.add(result);
				}
				FileUtils.writeStringToFile(new File("target/results/" + version + "-" + group + ".json"), collection.toJson(), "utf-8");
			}
		}

		JMHUtil.generateProvidedJson(path);
	}
}
