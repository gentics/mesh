package com.gentics.mesh.test.context;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class DummyTest {

	@Rule
	@ClassRule
	public static DummyRule rule = new DummyRule();

	@Test
	public void testA() {
		System.out.println("A");
	}

	@Test
	public void testB() {
		System.out.println("B");
	}
}
