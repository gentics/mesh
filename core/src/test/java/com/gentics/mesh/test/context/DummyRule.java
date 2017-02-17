package com.gentics.mesh.test.context;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class DummyRule extends TestWatcher {

	@Override
	protected void finished(Description description) {
		System.out.println(description.isTest());
		System.out.println("Finished");
	}

	@Override
	protected void starting(Description description) {
		System.out.println(description.isTest());
		System.out.println("Start");
	}
}
