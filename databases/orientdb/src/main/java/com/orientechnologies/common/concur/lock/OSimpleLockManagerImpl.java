package com.orientechnologies.common.concur.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.gentics.mesh.Mesh;
import com.orientechnologies.common.exception.OException;

public class OSimpleLockManagerImpl<T> implements OSimpleLockManager<T> {

	private final Lock lock = new ReentrantLock();
	private final Map<T, Condition> map = new HashMap<>();
	private final long timeout;

	public OSimpleLockManagerImpl(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public void lock(T key) {

//		System.out.println("Locking " + key + " on " + Mesh.mesh().getOptions().getNodeName() + " in thread " + Thread.currentThread());
		lock.lock();
		try {
			try {

				Condition c;
				do {
					c = map.get(key);
					if (c != null) {
						if (timeout == 0) {
							c.await();
						} else {
							if (!c.await(timeout, TimeUnit.MILLISECONDS)) {
								System.out.println("Lock timeout for " + key + " on " + Mesh.mesh().getOptions().getNodeName() + " in thread "
									+ Thread.currentThread());
								throw new OLockException(String.format("Time out acquire lock for resource: '%s' ", key));
							}
						}
					}
				} while (c != null);
				c = lock.newCondition();
				map.put(key, c);
			} catch (InterruptedException e) {
				throw OException.wrapException(new OInterruptedException("Interrupted Lock"), e);
			}
		} finally {
			lock.unlock();
		}

	}

	@Override
	public void unlock(T key) {
//		System.out.println("Unlocking " + key + " on " + Mesh.mesh().getOptions().getNodeName());
		lock.lock();
		try {
			Condition c = map.remove(key);
			assert c != null;
			c.signalAll();
		} finally {
			lock.unlock();
		}
	}
}
