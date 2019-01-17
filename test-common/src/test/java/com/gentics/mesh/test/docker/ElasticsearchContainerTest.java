package com.gentics.mesh.test.docker;

import org.junit.ClassRule;
import org.junit.Test;

public class ElasticsearchContainerTest {

	@ClassRule
	public static ElasticsearchContainer server = new ElasticsearchContainer(true);

	@Test
	public void testContainer() {

	}
}
