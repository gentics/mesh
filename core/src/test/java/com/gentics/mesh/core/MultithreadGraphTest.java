package com.gentics.mesh.core;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, useTinyDataset = false, startServer = false)
public class MultithreadGraphTest extends AbstractMeshTest {

	@Test
	public void testMultithreading() throws InterruptedException {

		runAndWait(() -> {
			try (Tx tx = db().tx()) {
				MeshRoot meshRoot = boot().meshRoot();
				User user = meshRoot.getUserRoot().create("test", null);
				assertNotNull(user);
				tx.success();
			}
			System.out.println("Created user");
		});

		runAndWait(() -> {
			try (Tx tx = db().tx()) {
				// fg.getEdges();
				runAndWait(() -> {
					User user = boot().meshRoot().getUserRoot().findByUsername("test");
					assertNotNull(user);
				});
				User user = boot().meshRoot().getUserRoot().findByUsername("test");
				assertNotNull(user);
				System.out.println("Read user");

			}
		});

		try (Tx tx = db().tx()) {
			User user = boot().meshRoot().getUserRoot().findByUsername("test");
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
