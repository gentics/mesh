/*
 * Copyright (c) 2011-2018 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.spi.cluster.hazelcast;

import com.hazelcast.core.PartitionService;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.ext.healthchecks.Status;

import java.util.Objects;

/**
 * A helper to create Vert.x cluster {@link io.vertx.ext.healthchecks.HealthChecks} procedures.
 */
@VertxGen
public interface ClusterHealthCheck {

  /**
   * Creates a ready-to-use Vert.x cluster {@link io.vertx.ext.healthchecks.HealthChecks} procedure.
   *
   * @param vertx the instance of Vert.x, must not be {@code null}
   * @return a Vert.x cluster {@link io.vertx.ext.healthchecks.HealthChecks} procedure
   */
  static Handler<Promise<Status>> createProcedure(Vertx vertx) {
    Objects.requireNonNull(vertx);
    return future -> {
      VertxInternal vertxInternal = (VertxInternal) vertx;
      HazelcastClusterManager clusterManager = (HazelcastClusterManager) vertxInternal.getClusterManager();
      PartitionService partitionService = clusterManager.getHazelcastInstance().getPartitionService();
      future.complete(new Status().setOk(partitionService.isClusterSafe()));
    };
  }
}
