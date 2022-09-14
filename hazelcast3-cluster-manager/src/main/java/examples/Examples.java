/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package examples;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.spi.cluster.hazelcast.ClusterHealthCheck;
import io.vertx.spi.cluster.hazelcast.ConfigUtil;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 *  @author <a href="http://tfox.org">Tim Fox</a>
 */
public class Examples {

  public void example1() {

    ClusterManager mgr = new HazelcastClusterManager();

    VertxOptions options = new VertxOptions().setClusterManager(mgr);

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
      } else {
        // failed!
      }
    });
  }

  public void example2() {

    Config hazelcastConfig = new Config();

    // Now set some stuff on the config (omitted)

    ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);

    VertxOptions options = new VertxOptions().setClusterManager(mgr);

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
      } else {
        // failed!
      }
    });
  }

  public void customizeDefaultConfig() {
    Config hazelcastConfig = ConfigUtil.loadConfig();

    hazelcastConfig.getGroupConfig()
      .setName("my-cluster-name");

    ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);

    VertxOptions options = new VertxOptions().setClusterManager(mgr);

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
      } else {
        // failed!
      }
    });
  }

  public void example3(HazelcastInstance hazelcastInstance) {
    ClusterManager mgr = new HazelcastClusterManager(hazelcastInstance);
    VertxOptions options = new VertxOptions().setClusterManager(mgr);
    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
      } else {
        // failed!
      }
    });
  }

  public void healthCheck(Vertx vertx) {
    Handler<Promise<Status>> procedure = ClusterHealthCheck.createProcedure(vertx);
    HealthChecks checks = HealthChecks.create(vertx).register("cluster-health", procedure);
  }

  public void healthCheckHandler(Vertx vertx, HealthChecks checks) {
    Router router = Router.router(vertx);
    router.get("/readiness").handler(HealthCheckHandler.createWithHealthChecks(checks));
  }

  public void liteMemberConfig() {
    Config hazelcastConfig = ConfigUtil.loadConfig()
      .setLiteMember(true);

    ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);

    VertxOptions options = new VertxOptions().setClusterManager(mgr);

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
      } else {
        // failed!
      }
    });
  }
}
