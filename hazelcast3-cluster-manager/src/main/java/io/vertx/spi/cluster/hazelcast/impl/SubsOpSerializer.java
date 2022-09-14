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

import io.vertx.core.Promise;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.TaskQueue;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.spi.cluster.RegistrationInfo;

import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

/**
 * @author Thomas Segismont
 */
public class SubsOpSerializer {

  private final VertxInternal vertx;
  private final TaskQueue taskQueue;

  private SubsOpSerializer(VertxInternal vertx) {
    this.vertx = vertx;
    taskQueue = new TaskQueue();
  }

  public static SubsOpSerializer get(ContextInternal context) {
    ConcurrentMap<Object, Object> contextData = context.contextData();
    SubsOpSerializer instance = (SubsOpSerializer) contextData.get(SubsOpSerializer.class);
    if (instance == null) {
      SubsOpSerializer candidate = new SubsOpSerializer(context.owner());
      SubsOpSerializer previous = (SubsOpSerializer) contextData.putIfAbsent(SubsOpSerializer.class, candidate);
      instance = previous == null ? candidate : previous;
    }
    return instance;
  }

  public void execute(BiConsumer<String, RegistrationInfo> op, String address, RegistrationInfo registrationInfo, Promise<Void> promise) {
    taskQueue.execute(() -> {
      try {
        op.accept(address, registrationInfo);
        promise.complete();
      } catch (Exception e) {
        promise.fail(e);
      }
    }, vertx.getWorkerPool().executor());
  }
}
