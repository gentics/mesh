package com.gentics.mesh.core.verticle.handler;

import java.util.concurrent.Semaphore;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;

@Singleton
public class WriteLockImpl implements WriteLock {

	private final Semaphore lock = new Semaphore(1);
	private final Database database;
	private MeshOptions options;

	@Inject
	public WriteLockImpl(MeshOptions options, Database database) {
		this.options = options;
		this.database = database;
	}

	@Override
	public void close() {
		unlock();
	}

	/**
	 * Locks writes. Use this to prevent concurrent write transactions.
	 */
	@Override
	public WriteLock lock() {
		boolean syncWrites = options.getStorageOptions().isSynchronizeWrites();
		database.blockingTopologyLockCheck();
		if (syncWrites) {
			try {
				lock.acquire();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return this;
	}

	/**
	 * Releases the lock that was acquired in {@link #lock()}.
	 */
	public void unlock() {
		lock.release();
	}

}
