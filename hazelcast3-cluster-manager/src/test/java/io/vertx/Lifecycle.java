/*
 * Copyright 2018 Red Hat, Inc.
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

package io.vertx;

import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.spi.cluster.WrappedClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

/**
 * @author Thomas Segismont
 */
public class Lifecycle {

  private static final Logger log = LoggerFactory.getLogger(Lifecycle.class);

  public static void closeClustered(List<Vertx> clustered) throws Exception {
    for (Vertx vertx : clustered) {
      VertxInternal vertxInternal = (VertxInternal) vertx;

      HazelcastClusterManager clusterManager = getHazelcastClusterManager(vertxInternal.getClusterManager());

      if (clusterManager != null) {
        HazelcastInstance hazelcastInstance = clusterManager.getHazelcastInstance();

        SECONDS.sleep(2); // Make sure rebalancing has been triggered

        long start = System.currentTimeMillis();
        try {
          while (!hazelcastInstance.getPartitionService().isClusterSafe()
            && System.currentTimeMillis() - start < MILLISECONDS.convert(2, MINUTES)) {
            MILLISECONDS.sleep(100);
          }
        } catch (Exception ignore) {
        }
      }

      CountDownLatch latch = new CountDownLatch(1);
      vertxInternal.close(ar -> {
        if (ar.failed()) {
          log.error("Failed to shutdown vert.x", ar.cause());
        }
        latch.countDown();
      });
      latch.await(2, TimeUnit.MINUTES);
    }
  }

  private static HazelcastClusterManager getHazelcastClusterManager(ClusterManager cm) {
    if (cm == null) {
      return null;
    }
    if (cm instanceof WrappedClusterManager) {
      return getHazelcastClusterManager(((WrappedClusterManager) cm).getDelegate());
    }
    if (cm instanceof HazelcastClusterManager) {
      return (HazelcastClusterManager) cm;
    }
    throw new ClassCastException("Unexpected cluster manager implementation: " + cm.getClass());
  }

  public static void closeDataNodes(List<HazelcastInstance> dataNodes) throws Exception {
    for (HazelcastInstance dataNode : dataNodes) {
      dataNode.getLifecycleService().terminate();
    }
  }

  private Lifecycle() {
    // Utility
  }
}
