package com.gentics.mesh.core;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mda.ATx;
import com.gentics.mda.entity.AUser;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class MultithreadGraphTest extends AbstractMeshTest {

	@Test
	public void testMultithreading() throws InterruptedException {

		runAndWait(() -> {
			try (ATx tx = tx()) {
				MeshRoot meshRoot = boot().meshRoot();
				AUser user = tx.users().create("test", null);
				user.setCreated(auser());
				assertNotNull(user);
				tx.success();
			}
			System.out.println("Created user");
		});

		runAndWait(() -> {
			try (ATx tx = tx()) {
				// fg.getEdges();
				runAndWait(() -> {
					AUser user = tx.users().findByUsername("test");
					assertNotNull(user);
				});
				AUser user = tx.users().findByUsername("test");
				assertNotNull(user);
				System.out.println("Read user");

			}
		});

		try (ATx tx = tx()) {
			AUser user = tx.users().findByUsername("test");
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
