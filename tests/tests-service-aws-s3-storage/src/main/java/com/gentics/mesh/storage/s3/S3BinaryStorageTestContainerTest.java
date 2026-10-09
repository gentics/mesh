package com.gentics.mesh.storage.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.gentics.mesh.test.docker.AWSContainer;

/**
 * Test for the lifecycle of the S3 containers, which are started by {@link S3BinaryStorageTest}.
 */
public class S3BinaryStorageTestContainerTest {

    @Test
    public void testS3ContainersAreStoppedAfterTests() {
        Set<String> before = AWSContainer.runningContainerIds();

        Result result = JUnitCore.runClasses(S3BinaryStorageTest.class);
        assertTrue("The tests using the S3 containers should succeed: " + result.getFailures(), result.wasSuccessful());
        assertEquals("All tests using the S3 containers should have been run", 3, result.getRunCount());

        Set<String> leaked = AWSContainer.runningContainerIds();
        leaked.removeAll(before);
        assertTrue("The S3 containers " + leaked + " should have been stopped after the tests", leaked.isEmpty());
    }
}
