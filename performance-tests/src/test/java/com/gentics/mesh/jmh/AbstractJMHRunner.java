package com.gentics.mesh.jmh;

import java.io.File;
import java.io.IOException;

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
			throw new RuntimeException("Invalid arguments. Only the name of the benchmark output file can/must be specified.");
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
			JMHUtil.generateProvidedJson(OUTPUT_DIR.toPath());
		}
	}

	public static void start(String name, Class<?> clazz) throws RunnerException, IOException {
		Options options = create(new String[] { name })
			.include(clazz.getSimpleName())
			.build();
		run(options, true);
	}

}