package com.orientechnologies.orient.server.distributed.impl;

import com.orientechnologies.common.concur.lock.OSimpleLockManagerImpl;

public class LockLessLockManager<T> extends OSimpleLockManagerImpl<T> {

	public LockLessLockManager(long timeout) {
		super(timeout);
	}

}
