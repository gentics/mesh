package com.gentics.mesh.core.verticle.handler;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;

@Singleton
public class WriteLockImpl implements WriteLock {

	private final Semaphore lock = new Semaphore(1);
	private final Database database;
	private final MeshOptions options;

	@Inject
	public WriteLockImpl(MeshOptions options, Database database) {
		this.options = options;
		this.database = database;
	}

	@Override
	public void close() {
		lock.release();
	}

	/**
	 * Locks writes. Use this to prevent concurrent write transactions.
	 */
	@Override
	public WriteLock lock(InternalActionContext ac) {
		if (ac.isSkipWriteLock()) {
			return this;
		} else {
			boolean syncWrites = options.getStorageOptions().isSynchronizeWrites();
			database.blockingTopologyLockCheck();
			if (syncWrites) {
				try {
					boolean isTimeout = !lock.tryAcquire(options.getStorageOptions().getSynchronizeWritesTimeout(), TimeUnit.MILLISECONDS);
					if (isTimeout) {
						throw new RuntimeException("Got timeout while waiting for write lock.");
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			return this;
		}
	}

}
