package com.gentics.mesh.server.cluster.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.gentics.madl.tx.TxAction;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.server.cluster.ClusterServer;
import com.gentics.mesh.server.cluster.test.task.LoadTask;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;

public abstract class AbstractClusterTest extends ClusterServer {

	static {
		// Disable direct IO (My dev system uses ZFS. Otherwise the test will not run)
		System.setProperty("storage.wal.allowDirectIO", "false");
	}

	// Define the test parameter
	static final long txDelay = 0;
	static final boolean lockTx = false;
	static final boolean lockForDBSync = false;

	protected Database db;

	public final List<Object> productIds = new ArrayList<>();

	public List<Object> categoryIds = new ArrayList<>();

	protected static Mesh mesh;

	private static MeshComponent internal;

	public static void setup(MeshOptions options) throws Exception {
		mesh = Mesh.create(options);
		mesh.run(false);
		Thread.sleep(5000);
		internal = (MeshComponent) mesh.internal();
	}

	public void initDB(String name, String graphDbBasePath, String httpPort, String binPort) throws Exception {
		OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(Integer.MAX_VALUE);
		OGlobalConfiguration.DISTRIBUTED_BACKUP_DIRECTORY.setValue("target/backup_" + name);
	}

//	public <T> T tx(Function<TransactionalGraph, T> handler) {
//		TransactionalGraph tx = internal.database().rawTx();
//		try {
//			try {
//				T result = handler.apply(tx);
//				tx.commit();
//				return result;
//			} catch (Exception e) {
//				e.printStackTrace();
//				// Explicitly invoke rollback as suggested by luigidellaquila
//				tx.rollback();
//				throw e;
//			}
//		} finally {
//			tx.shutdown();
//		}
//	}

//	public void tx(Consumer<TransactionalGraph> handler) {
//		tx(tx -> {
//			handler.accept(tx);
//			return null;
//		});
//	}

	public <T> T tx(TxAction<T> action) {
		return internal.database().tx(action);
	}
	
	public void triggerSlowLoad(LoadTask task) throws Exception {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
		System.out.println("Press any key to start load");
		System.in.read();
		System.out.println("Invoking task execution #1");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 5000, TimeUnit.MILLISECONDS);
	}

	public void triggerLoad(LoadTask task) throws Exception {

		// Now continue to invoke task
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
		System.out.println("Press any key to start load");
		System.in.read();
		System.out.println("Invoking task execution #1");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 500, TimeUnit.MILLISECONDS);
		System.in.read();
		System.out.println("Invoking task execution #2");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		System.out.println("Invoking task execution #3");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		System.out.println("Invoking task execution #4");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 20, TimeUnit.MILLISECONDS);

		System.out.println("Press any key to shutdown the execution");
		System.in.read();
		System.out.println("Stopping threads.");
		executor.shutdown();
		Thread.sleep(1000);
		System.out.println("Timer stopped.");
		System.out.println(
			"Press any key to update product one more time. This time no lock error should occure since the other TX's have been terminated.");

		System.in.read();
		task.runTask(txDelay, lockTx, lockForDBSync);
	}

	public static void waitAndShutdown() throws Exception {
		System.out.println("Press any key to shutdown the instance");
		System.in.read();
		Utils.sleep(5000);
		mesh.shutdown();
	}

	public Database getDb() {
		return db;
	}

}
