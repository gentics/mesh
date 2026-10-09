package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.AWSTestMode.RUSTFS;
import static com.gentics.mesh.test.TestSize.EMPTY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.docker.AWSContainer;

/**
 * Test for the lifecycle of the S3 container, which is started by the {@link MeshTestContext} for test classes using
 * {@link MeshTestSetting#awsContainer()}.
 */
public class MeshTestContextAWSContainerTest {

	private static final Set<String> CONTAINERS_DURING_TEST = new HashSet<>();

	@Test
	public void testS3ContainerIsStoppedAfterTestClass() {
		Set<String> before = AWSContainer.runningContainerIds();

		Result result = JUnitCore.runClasses(S3ContainerTestCase.class);
		assertTrue("The test class using the S3 container should succeed: " + result.getFailures(), result.wasSuccessful());

		Set<String> started = new HashSet<>(CONTAINERS_DURING_TEST);
		started.removeAll(before);
		assertFalse("An S3 container should have been started for the test class", started.isEmpty());

		Set<String> leaked = AWSContainer.runningContainerIds();
		leaked.retainAll(started);
		assertTrue("The S3 containers " + leaked + " should have been stopped after the test class", leaked.isEmpty());
	}

	/**
	 * Test class, which uses the S3 container. It is run by {@link MeshTestContextAWSContainerTest#testS3ContainerIsStoppedAfterTestClass()} only.
	 */
	@MeshTestSetting(awsContainer = RUSTFS, testSize = EMPTY, startServer = false)
	public static class S3ContainerTestCase extends AbstractMeshTest {

		@Test
		public void testS3ContainerRunning() {
			CONTAINERS_DURING_TEST.addAll(AWSContainer.runningContainerIds());
		}
	}
}
