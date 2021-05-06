package com.gentics.mesh.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

import io.vertx.core.Vertx;
import net.lingala.zip4j.exception.ZipException;

public class ElasticsearchProcessManagerTest {
	
	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

	private static String OS = System.getProperty("os.name").toLowerCase();

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	@Test
	public void testExecFailure() throws Exception {
		environmentVariables.set("JAVA_HOME", "/bogus");
		ElasticsearchProcessManager manager = new ElasticsearchProcessManager(Vertx.vertx(), new ElasticSearchOptions());
		try {
			manager.start();
			fail("An error should be thrown");
		} catch (Exception e) {
			String expectedPath = "/bogus/bin/java";
			if(isWindows()) {
				expectedPath = "C:\\bogus\\bin\\java.exe";
			}
			String expectedMessage = String.format("Could not find java executable using JAVA_HOME {/bogus} - Was looking in {%s}", expectedPath);
			String msg = e.getMessage();
			assertEquals(expectedMessage, msg);
		}
	}

	@Test
	public void testExec() throws IOException, ZipException, InterruptedException {
		ElasticsearchProcessManager manager = new ElasticsearchProcessManager(Vertx.vertx(), new ElasticSearchOptions());
		Process p = manager.start();
		assertEquals(p, manager.getProcess());
		assertTrue("Process should have been started.", p.isAlive());

		// Start the watchdog and stop the process directly. The watchdog must restart the process.
		manager.startWatchDog();
		p.destroyForcibly();
		Thread.sleep(14000);
		assertTrue("The process should have been restarted.", manager.getProcess().isAlive());

		// Stop the watchdog and check that the process was not restarted
		manager.stopWatchDog();
		manager.getProcess().destroyForcibly();
		Thread.sleep(6000);
		assertFalse("The process should be still stopped.", manager.getProcess().isAlive());

		// Start again and assert that regular stop works
		p = manager.start();
		Thread.sleep(1000);
		assertTrue("The process should have been started.", p.isAlive());
		Thread.sleep(1000);
		manager.stop();
		Thread.sleep(2000);
		assertNull("The process object should be null since the process has been stopped.", manager.getProcess());
		assertFalse("The process should have been stopped.", p.isAlive());
	}

}
