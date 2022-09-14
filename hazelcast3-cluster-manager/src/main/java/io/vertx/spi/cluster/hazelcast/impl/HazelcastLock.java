/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.spi.cluster.hazelcast.impl;

import com.hazelcast.core.ISemaphore;
import io.vertx.core.shareddata.Lock;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class HazelcastLock implements Lock {

  private final ISemaphore semaphore;
  private final Executor lockReleaseExec;
  private final AtomicBoolean released = new AtomicBoolean();

  public HazelcastLock(ISemaphore semaphore, Executor lockReleaseExec) {
    this.semaphore = semaphore;
    this.lockReleaseExec = lockReleaseExec;
  }

  @Override
  public void release() {
    if (released.compareAndSet(false, true)) {
      lockReleaseExec.execute(semaphore::release);
    }
  }
}
