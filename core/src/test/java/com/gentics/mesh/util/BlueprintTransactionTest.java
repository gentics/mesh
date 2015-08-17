package com.gentics.mesh.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.graphdb.BlueprintTransaction;
import com.gentics.mesh.test.AbstractDBTest;

public class BlueprintTransactionTest extends AbstractDBTest {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testTransaction() throws InterruptedException {
		AtomicInteger i = new AtomicInteger(0);

		UserRoot root = meshRoot().getUserRoot();
		int e = i.incrementAndGet();
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			assertNotNull(root.create("testuser" + e, group(), user()));
			assertNotNull(boot.userRoot().findByUsername("testuser" + e));
			tx.success();
		}
		assertNotNull(boot.userRoot().findByUsername("testuser" + e));

		int u = i.incrementAndGet();
		Runnable task = () -> {
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				assertNotNull(root.create("testuser" + u, group(), user()));
				assertNotNull(boot.userRoot().findByUsername("testuser" + u));
				tx.failure();
			}
			assertNull(boot.userRoot().findByUsername("testuser" + u));

		};
		Thread t = new Thread(task);
		t.start();
		t.join();

		assertNull(boot.userRoot().findByUsername("testuser" + u));
		System.out.println("RUN: " + i.get());

	}

	@Test
	public void testMultiThreadedModifications() throws InterruptedException {
		User user = user();

		Runnable task2 = () -> {
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				user.setUsername("test2");
				assertNotNull(boot.userRoot().findByUsername("test2"));
				tx.success();
			}
			assertNotNull(boot.userRoot().findByUsername("test2"));

			Runnable task = () -> {
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					user.setUsername("test3");
					assertNotNull(boot.userRoot().findByUsername("test3"));
					tx.failure();
				}
				assertNotNull(boot.userRoot().findByUsername("test2"));
				assertNull(boot.userRoot().findByUsername("test3"));

			};
			Thread t = new Thread(task);
			t.start();
			try {
				t.join();
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		Thread t2 = new Thread(task2);
		t2.start();
		t2.join();
		assertNull(boot.userRoot().findByUsername("test3"));
		assertNotNull(boot.userRoot().findByUsername("test2"));

	}
}
