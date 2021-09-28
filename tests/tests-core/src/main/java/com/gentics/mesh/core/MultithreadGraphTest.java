package com.gentics.mesh.core;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = false)
public class MultithreadGraphTest extends AbstractMeshTest {

	@Test
	public void testMultithreading() throws InterruptedException {

		runAndWait(() -> {
			try (Tx tx = tx()) {
				UserDao userDao = tx.userDao();
				HibUser user = userDao.create("test", null);
				user.setCreated(user());
				assertNotNull(user);
				tx.success();
			}
			System.out.println("Created user");
		});

		runAndWait(() -> {
			try (Tx tx = tx()) {
				// fg.getEdges();
				runAndWait(() -> {
					HibUser user = boot().userDao().findByUsername("test");
					assertNotNull(user);
				});
				HibUser user = boot().userDao().findByUsername("test");
				assertNotNull(user);
				System.out.println("Read user");

			}
		});

		try (Tx tx = tx()) {
			HibUser user = boot().userDao().findByUsername("test");
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
