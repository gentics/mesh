package com.gentics.mesh.server.cluster.test.task;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.server.cluster.test.AbstractClusterTest;
import com.hazelcast.core.HazelcastInstance;

/**
 * Update cases:
 * 
 * A) Update existing vertex property
 * 
 * B) Add new edge between existing vertices
 * 
 * C) Add new edge to new vertex
 * 
 * D) Delete a random vertex
 */
public class BackupTask extends AbstractLoadTask {

	public BackupTask(AbstractClusterTest test) {
		super(test);
	}

	@Override
	public void runTask(long txDelay, boolean lockTx, boolean lockForDBSync) {
		Lock lock = null;
		if (lockTx) {
			HazelcastInstance hz = ((OrientDBClusterManager) test.getDb().clusterManager()).getHazelcast();
			lock = hz.getLock(WriteLock.GLOBAL_LOCK_KEY);
			lock.lock();
		}
		try {
			test.getDb().backupDatabase("target/backups");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}

}
