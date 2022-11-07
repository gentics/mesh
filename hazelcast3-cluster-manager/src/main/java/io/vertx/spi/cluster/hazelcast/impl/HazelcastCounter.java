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

import com.hazelcast.core.IAtomicLong;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.shareddata.Counter;

import java.util.Objects;

public class HazelcastCounter implements Counter {

  private final VertxInternal vertx;
  private final IAtomicLong atomicLong;

  public HazelcastCounter(VertxInternal vertx, IAtomicLong atomicLong) {
    this.vertx = vertx;
    this.atomicLong = atomicLong;
  }

  @Override
  public Future<Long> get() {
    Promise<Long> promise = vertx.promise();
    atomicLong.getAsync().andThen(new HandlerCallBackAdapter<>(promise));
    return promise.future();
  }

  @Override
  public Future<Long> incrementAndGet() {
    Promise<Long> promise = vertx.promise();
    atomicLong.incrementAndGetAsync().andThen(new HandlerCallBackAdapter<>(promise));
    return promise.future();
  }

  @Override
  public Future<Long> getAndIncrement() {
    Promise<Long> promise = vertx.promise();
    atomicLong.getAndIncrementAsync().andThen(new HandlerCallBackAdapter<>(promise));
    return promise.future();
  }

  @Override
  public Future<Long> decrementAndGet() {
    Promise<Long> promise = vertx.promise();
    atomicLong.decrementAndGetAsync().andThen(new HandlerCallBackAdapter<>(promise));
    return promise.future();
  }

  @Override
  public Future<Long> addAndGet(long value) {
    Promise<Long> promise = vertx.promise();
    atomicLong.addAndGetAsync(value).andThen(new HandlerCallBackAdapter<>(promise));
    return promise.future();
  }

  @Override
  public Future<Long> getAndAdd(long value) {
    Promise<Long> promise = vertx.promise();
    atomicLong.getAndAddAsync(value).andThen(new HandlerCallBackAdapter<>(promise));
    return promise.future();
  }

  @Override
  public Future<Boolean> compareAndSet(long expected, long value) {
    Promise<Boolean> promise = vertx.promise();
    atomicLong.compareAndSetAsync(expected, value).andThen(new HandlerCallBackAdapter<>(promise));
    return promise.future();
  }

  @Override
  public void get(Handler<AsyncResult<Long>> resultHandler) {
    Objects.requireNonNull(resultHandler, "resultHandler");
    get().onComplete(resultHandler);
  }

  @Override
  public void incrementAndGet(Handler<AsyncResult<Long>> resultHandler) {
    Objects.requireNonNull(resultHandler, "resultHandler");
    incrementAndGet().onComplete(resultHandler);
  }

  @Override
  public void getAndIncrement(Handler<AsyncResult<Long>> resultHandler) {
    Objects.requireNonNull(resultHandler, "resultHandler");
    getAndIncrement().onComplete(resultHandler);
  }

  @Override
  public void decrementAndGet(Handler<AsyncResult<Long>> resultHandler) {
    Objects.requireNonNull(resultHandler, "resultHandler");
    decrementAndGet().onComplete(resultHandler);
  }

  @Override
  public void addAndGet(long value, Handler<AsyncResult<Long>> resultHandler) {
    Objects.requireNonNull(resultHandler, "resultHandler");
    addAndGet(value).onComplete(resultHandler);
  }

  @Override
  public void getAndAdd(long value, Handler<AsyncResult<Long>> resultHandler) {
    Objects.requireNonNull(resultHandler, "resultHandler");
    getAndAdd(value).onComplete(resultHandler);
  }

  @Override
  public void compareAndSet(long expected, long value, Handler<AsyncResult<Boolean>> resultHandler) {
    Objects.requireNonNull(resultHandler, "resultHandler");
    compareAndSet(expected, value).onComplete(resultHandler);
  }
}
