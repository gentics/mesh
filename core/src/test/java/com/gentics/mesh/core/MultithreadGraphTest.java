package com.gentics.mesh.core;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractDBTest;

public class MultithreadGraphTest extends AbstractDBTest {

	@Before
	public void cleanup() {
		resetDatabase();
	}

	@Test
	public void testMultithreading() throws InterruptedException {

		runAndWait(() -> {
			try (Trx tx = db.trx()) {
				MeshRoot meshRoot = boot.meshRoot();
				User user = meshRoot.getUserRoot().create("test", null);
				assertNotNull(user);
				tx.success();
			}
			System.out.println("Created user");
		});

		runAndWait(() -> {
			try (Trx tx = db.trx()) {
				// fg.getEdges();
				runAndWait(() -> {
					User user = boot.meshRoot().getUserRoot().findByUsername("test");
					assertNotNull(user);
				});
				User user = boot.meshRoot().getUserRoot().findByUsername("test");
				assertNotNull(user);
				System.out.println("Read user");

			}
		});

		try (Trx tx = db.trx()) {
			User user = boot.meshRoot().getUserRoot().findByUsername("test");
			assertNotNull(user);
		}
	}

	public void runAndWait(Runnable runnable) {
		Thread thread = new Thread(runnable);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done waiting");
	}
}
