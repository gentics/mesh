package com.gentics.mesh.jmh.test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;

import com.gentics.mesh.jmh.AbstractJMHRunner;
import com.gentics.mesh.test.context.MeshTestContext;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class MyBenchmark extends AbstractJMHRunner {

	// @TearDown(Level.Iteration)
	MeshTestContext testContext;

	@TearDown
	public void tearDown() {
		System.out.println("Teardown");
	}

	// @Setup(Level.Iteration)
	@Setup
	public void setup() {
		System.out.println("Setup");
		testContext = new MeshTestContext();
	}

	@Benchmark
	public void testMethod() {
		try {
			Thread.sleep((long) (Math.random() * 100));
		} catch (InterruptedException e) {
		}
	}

	public static void main(String[] args) throws RunnerException, IOException {
		start("myBenchmark", MyBenchmark.class);
	}

}