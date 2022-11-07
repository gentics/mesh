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

package io.vertx.core;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.LoggingTestWatcher;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import io.vertx.test.core.AsyncTestBase;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ProgrammaticHazelcastClusterManagerTest extends AsyncTestBase {

  static {
    // this is only checked once every 10 seconds by Hazelcast on client disconnect
    System.setProperty("hazelcast.client.max.no.heartbeat.seconds", "9");
  }

  @Rule
  public LoggingTestWatcher watchman = new LoggingTestWatcher();

  @Override
  public void setUp() throws Exception {
    Random random = new Random();
    System.setProperty("vertx.hazelcast.test.group.name", new BigInteger(128, random).toString(32));
    super.setUp();
  }

  @Test
  public void testProgrammaticSetConfig() throws Exception {
    Config config = createConfig();
    HazelcastClusterManager mgr = new HazelcastClusterManager();
    mgr.setConfig(config);
    testProgrammatic(mgr, config);
  }

  @Test
  public void testProgrammaticSetWithConstructor() throws Exception {
    Config config = createConfig();
    HazelcastClusterManager mgr = new HazelcastClusterManager(config);
    testProgrammatic(mgr, config);
  }

  @Test
  public void testCustomHazelcastInstance() throws Exception {
    HazelcastInstance instance = Hazelcast.newHazelcastInstance(createConfig());
    HazelcastClusterManager mgr = new HazelcastClusterManager(instance);
    testProgrammatic(mgr, instance.getConfig());
  }

  private Config createConfig() {
    return new Config()
      .setProperty("hazelcast.wait.seconds.before.join", "0")
      .setProperty("hazelcast.local.localAddress", "127.0.0.1")
      .setGroupConfig(new GroupConfig()
        .setName(System.getProperty("vertx.hazelcast.test.group.name")));
  }

  private void testProgrammatic(HazelcastClusterManager mgr, Config config) throws Exception {
    mgr.setConfig(config);
    assertEquals(config, mgr.getConfig());
    VertxOptions options = new VertxOptions().setClusterManager(mgr);
    Vertx.clusteredVertx(options, res -> {
      assertTrue(res.succeeded());
      assertNotNull(mgr.getHazelcastInstance());
      res.result().close(res2 -> {
        assertTrue(res2.succeeded());
        testComplete();
      });
    });
    await();
  }

  @Test
  public void testEventBusWhenUsingACustomHazelcastInstance() throws Exception {
    HazelcastInstance instance1 = Hazelcast.newHazelcastInstance(createConfig());
    HazelcastInstance instance2 = Hazelcast.newHazelcastInstance(createConfig());

    HazelcastClusterManager mgr1 = new HazelcastClusterManager(instance1);
    HazelcastClusterManager mgr2 = new HazelcastClusterManager(instance2);
    VertxOptions options1 = new VertxOptions().setClusterManager(mgr1);
    options1.getEventBusOptions().setHost("127.0.0.1");
    VertxOptions options2 = new VertxOptions().setClusterManager(mgr2);
    options2.getEventBusOptions().setHost("127.0.0.1");

    AtomicReference<Vertx> vertx1 = new AtomicReference<>();
    AtomicReference<Vertx> vertx2 = new AtomicReference<>();

    Vertx.clusteredVertx(options1, res -> {
      assertTrue(res.succeeded());
      assertNotNull(mgr1.getHazelcastInstance());
      res.result().eventBus().consumer("news", message -> {
        assertNotNull(message);
        assertTrue(message.body().equals("hello"));
        testComplete();
      });
      vertx1.set(res.result());
    });

    assertWaitUntil(() -> vertx1.get() != null);

    Vertx.clusteredVertx(options2, res -> {
      assertTrue(res.succeeded());
      assertNotNull(mgr2.getHazelcastInstance());
      vertx2.set(res.result());
      res.result().eventBus().send("news", "hello");
    });

    await();

    vertx1.get().close(ar -> vertx1.set(null));
    vertx2.get().close(ar -> vertx2.set(null));

    assertTrue(instance1.getLifecycleService().isRunning());
    assertTrue(instance2.getLifecycleService().isRunning());

    assertWaitUntil(() -> vertx1.get() == null && vertx2.get() == null);

    instance1.shutdown();
    instance2.shutdown();

  }

  @Test
  public void testSharedDataUsingCustomHazelcast() throws Exception {
    HazelcastInstance instance1 = Hazelcast.newHazelcastInstance(createConfig());
    HazelcastInstance instance2 = Hazelcast.newHazelcastInstance(createConfig());

    HazelcastClusterManager mgr1 = new HazelcastClusterManager(instance1);
    HazelcastClusterManager mgr2 = new HazelcastClusterManager(instance2);
    VertxOptions options1 = new VertxOptions().setClusterManager(mgr1);
    options1.getEventBusOptions().setHost("127.0.0.1");
    VertxOptions options2 = new VertxOptions().setClusterManager(mgr2);
    options2.getEventBusOptions().setHost("127.0.0.1");

    AtomicReference<Vertx> vertx1 = new AtomicReference<>();
    AtomicReference<Vertx> vertx2 = new AtomicReference<>();

    Vertx.clusteredVertx(options1, res -> {
      assertTrue(res.succeeded());
      assertNotNull(mgr1.getHazelcastInstance());
      res.result().sharedData().getClusterWideMap("mymap1", ar -> {
        ar.result().put("news", "hello", v -> {
          vertx1.set(res.result());
        });
      });
    });

    assertWaitUntil(() -> vertx1.get() != null);

    Vertx.clusteredVertx(options2, res -> {
      assertTrue(res.succeeded());
      assertNotNull(mgr2.getHazelcastInstance());
      vertx2.set(res.result());
      res.result().sharedData().getClusterWideMap("mymap1", ar -> {
        ar.result().get("news", r -> {
          assertEquals("hello", r.result());
          testComplete();
        });
      });
    });

    await();

    vertx1.get().close(ar -> vertx1.set(null));
    vertx2.get().close(ar -> vertx2.set(null));

    assertWaitUntil(() -> vertx1.get() == null && vertx2.get() == null);

    // be sure stopping vertx did not cause or require our custom hazelcast to shutdown

    assertTrue(instance1.getLifecycleService().isRunning());
    assertTrue(instance2.getLifecycleService().isRunning());

    instance1.shutdown();
    instance2.shutdown();

  }

  @Test
  public void testThatExternalHZInstanceCanBeShutdown() {
    // This instance won't be used by vert.x
    HazelcastInstance instance = Hazelcast.newHazelcastInstance(createConfig());
    String nodeID = instance.getCluster().getLocalMember().getUuid();

    HazelcastClusterManager mgr = new HazelcastClusterManager(createConfig());
    VertxOptions options = new VertxOptions().setClusterManager(mgr);
    options.getEventBusOptions().setHost("127.0.0.1");

    AtomicReference<Vertx> vertx1 = new AtomicReference<>();

    Vertx.clusteredVertx(options, res -> {
      assertTrue(res.succeeded());
      assertNotNull(mgr.getHazelcastInstance());
      res.result().sharedData().getClusterWideMap("mymap1", ar -> {
        ar.result().put("news", "hello", v -> {
          vertx1.set(res.result());
        });
      });
    });

    assertWaitUntil(() -> vertx1.get() != null);
    int size = mgr.getNodes().size();
    assertTrue(mgr.getNodes().contains(nodeID));

    // Retrieve the value inserted by vert.x
    Map<Object, Object> map = instance.getMap("mymap1");
    Map<Object, Object> anotherMap = instance.getMap("mymap2");
    assertEquals(map.get("news"), "hello");
    map.put("another-key", "stuff");
    anotherMap.put("another-key", "stuff");
    map.remove("news");
    map.remove("another-key");
    anotherMap.remove("another-key");

    instance.shutdown();

    assertWaitUntil(() -> mgr.getNodes().size() == size - 1);
    vertx1.get().close(ar -> vertx1.set(null));

    assertWaitUntil(() -> vertx1.get() == null);
  }

  @AfterClass
  public static void afterTests() {
    System.clearProperty("hazelcast.client.max.no.heartbeat.seconds");
  }
}
