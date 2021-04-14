package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.assertj.core.data.Percentage;

import com.gentics.mesh.test.performance.BenchmarkJob;
import com.gentics.mesh.test.performance.StopWatch;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class PerformanceTestUtils {

	private static final Logger log = LoggerFactory.getLogger(PerformanceTestUtils.class);

	private static long currentMark;

	/**
	 * @deprecated Use {@link StopWatch}
	 */
	@Deprecated
	public static void mark() {
		currentMark = System.currentTimeMillis();
	}

	public static void measureAndAssert(int nRuns, float expectedFactor, float allowedVariation) {
		long duration = System.currentTimeMillis() - currentMark;
		// System.out.println(duration);
		float perRun = (duration / (float) nRuns);
		log.info("Average time per run: {" + perRun + "} ms");
		// System.out.println(perRun);
		float factor = perRun / computeBaseline();
		assertThat(expectedFactor).isCloseTo(factor, Percentage.withPercentage(allowedVariation));
		// System.out.println(factor);
	}

	public static float computeBaseline() {
		File file = new File("target", "benchmark.properties");
		Properties props = new Properties();
		if (file.exists()) {
			if (log.isDebugEnabled()) {
				log.debug("Found {" + file.getAbsolutePath() + "} loading benchmark info.");
			}
			try {
				props.load(new FileInputStream(file));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				int nThreads = 12;
				ExecutorService executor = Executors.newFixedThreadPool(nThreads);
				List<BenchmarkJob> jobs = new ArrayList<>();
				for (int i = 0; i < nThreads; i++) {
					BenchmarkJob job = new BenchmarkJob(38);
					jobs.add(job);
					executor.execute(job);
				}
				executor.shutdown();
				executor.awaitTermination(15, TimeUnit.SECONDS);

				long total = 0;
				for (BenchmarkJob job : jobs) {
					total += job.getDuration();
				}
				props.setProperty("score", String.valueOf(total / nThreads));
				props.store(new FileOutputStream(file), null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return Long.valueOf(props.getProperty("score"));

	}

}
