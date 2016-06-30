package com.gentics.mesh.test.performance;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class StopWatchLogger {

	private static final Logger log = LoggerFactory.getLogger(StopWatchLogger.class);

	private Class<?> clazz;

	private final DecimalFormat df = new DecimalFormat("#.####");

	private Map<String, Double> entries = new HashMap<>();

	public StopWatchLogger(Class<?> clazz) {
		this.clazz = clazz;
	}

	public static StopWatchLogger logger(Class<?> clazz) {
		return new StopWatchLogger(clazz);
	}

	public void log(String name, double timeInMs) {
		if (log.isDebugEnabled()) {
			log.debug("Logging {" + name + "} with time:" + df.format(timeInMs) + " [ms]");
		}
		entries.put(name, timeInMs);
	}

	public void flush() {
		Serializer serializer = new Persister();
		Testsuite example = new Testsuite(clazz, 0f);
		for (Entry<String, Double> entry : entries.entrySet()) {
			example.getTestcases().add(new Testcase(entry.getKey(), clazz.getName(), String.valueOf(entry.getValue()/(double)1000)));
		}
		File reportFile = new File("target", "TEST-" + clazz.getName() + ".performance.xml");
		try {
//			serializer.write(example, System.out);
			serializer.write(example, reportFile);
		} catch (Exception e) {
			log.error("Error while saving {" + reportFile.getAbsolutePath() + "} for {" + clazz.getName() + "}", e);
		}


	}

}
