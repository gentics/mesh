package com.gentics.mesh.jmh;

import java.io.IOException;

import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;

import com.gentics.mesh.core.node.GroupEndpointPerformanceTest;

public class JMHRunner extends AbstractJMHRunner {

	public static void main(String[] args) throws RunnerException, IOException {
		Options options = create(args)
			.include(GroupEndpointPerformanceTest.class.getSimpleName())
			.build();
		run(options, true);
	}

}