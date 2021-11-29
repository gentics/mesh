package com.gentics.mesh.server.cluster.test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.TxAction;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.server.cluster.ClusterServer;
import com.gentics.mesh.server.cluster.test.task.LoadTask;

public abstract class AbstractClusterTest extends ClusterServer {

	static {
		// Disable direct IO (My dev system uses ZFS. Otherwise the test will not run)
		System.setProperty("storage.wal.allowDirectIO", "false");
	}

	// Define the test parameter
	static final long txDelay = 0;
	static final boolean lockTx = true;
	static final boolean lockForDBSync = false;

	protected static Mesh mesh;

	private static MeshComponent internal;

	public static void setup(MeshOptions options) throws Exception {
		mesh = Mesh.create(options);
		mesh.run(false);
		while (mesh.getStatus() != MeshStatus.READY) {
			Thread.sleep(100);
		}
		internal = (MeshComponent) mesh.internal();
	}

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
		MeshComponent component = mesh.internal();
		return component.database();
	}

	public BootstrapInitializer getBoot() {
		MeshComponent component = mesh.internal();
		return component.boot();
	}

	public static Mesh getMesh() {
		return mesh;
	}

}
