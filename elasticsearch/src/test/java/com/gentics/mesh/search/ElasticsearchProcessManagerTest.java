package com.gentics.mesh.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

import io.vertx.core.Vertx;
import net.lingala.zip4j.exception.ZipException;

public class ElasticsearchProcessManagerTest {

	@Test
	public void testExecFailure() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("JAVA_HOME", "/bogus");
		set(map);
		ElasticsearchProcessManager manager = new ElasticsearchProcessManager(Vertx.vertx(), new ElasticSearchOptions());
		try {
			manager.start();
			fail("An error should be thrown");
		} catch (Exception e) {
			String msg = e.getMessage();
			assertEquals("Could not find java executable using JAVA_HOME {/bogus} - Was looking in {/bogus/bin/java}", msg);
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

		// Stop the watchdog and check that the proccess was not restarted
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

	/**
	 * Override the environment variables.
	 * 
	 * @param newenv
	 * @throws Exception
	 */
	public static void set(Map<String, String> newenv) throws Exception {
		Class[] classes = Collections.class.getDeclaredClasses();
		Map<String, String> env = System.getenv();
		for (Class cl : classes) {
			if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
				Field field = cl.getDeclaredField("m");
				field.setAccessible(true);
				Object obj = field.get(env);
				Map<String, String> map = (Map<String, String>) obj;
				map.clear();
				map.putAll(newenv);
			}
		}
	}

}
