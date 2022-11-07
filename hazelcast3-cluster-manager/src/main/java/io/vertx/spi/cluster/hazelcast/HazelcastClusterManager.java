/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.spi.cluster.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.spi.cluster.*;
import io.vertx.spi.cluster.hazelcast.impl.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * A cluster manager that uses Hazelcast
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class HazelcastClusterManager implements ClusterManager, MembershipListener, LifecycleListener {

  private static final Logger log = LoggerFactory.getLogger(HazelcastClusterManager.class);

  private static final String LOCK_SEMAPHORE_PREFIX = "__vertx.";
  private static final String NODE_ID_ATTRIBUTE = "__vertx.nodeId";

  private VertxInternal vertx;
  private NodeSelector nodeSelector;

  private HazelcastInstance hazelcast;
  private String nodeId;
  private NodeInfo nodeInfo;
  private SubsMapHelper subsMapHelper;
  private IMap<String, HazelcastNodeInfo> nodeInfoMap;
  private String membershipListenerId;
  private String lifecycleListenerId;
  private boolean customHazelcastCluster;
  private Set<String> nodeIds = new HashSet<>();

  private NodeListener nodeListener;
  private volatile boolean active;

  private Config conf;

  private ExecutorService lockReleaseExec;

  /**
   * Constructor - gets config from classpath
   */
  public HazelcastClusterManager() {
  }

  /**
   * Constructor - config supplied
   *
   * @param conf Hazelcast config, not null
   */
  public HazelcastClusterManager(Config conf) {
    Objects.requireNonNull(conf, "The Hazelcast config cannot be null.");
    this.conf = conf;
  }

  public HazelcastClusterManager(HazelcastInstance instance) {
    Objects.requireNonNull(instance, "The Hazelcast instance cannot be null.");
    hazelcast = instance;
    customHazelcastCluster = true;
  }

  @Override
  public void init(Vertx vertx, NodeSelector nodeSelector) {
    this.vertx = (VertxInternal) vertx;
    this.nodeSelector = nodeSelector;
  }

  @Override
  public void join(Promise<Void> promise) {
    vertx.executeBlocking(prom -> {
      if (!active) {
        active = true;

        lockReleaseExec = Executors.newCachedThreadPool(r -> new Thread(r, "vertx-hazelcast-service-release-lock-thread"));

        // The hazelcast instance has not been passed using the constructor.
        if (!customHazelcastCluster) {
          if (conf == null) {
            conf = loadConfig();
            if (conf == null) {
              log.warn("Cannot find cluster configuration on 'vertx.hazelcast.config' system property, on the classpath, " +
                "or specified programmatically. Using default hazelcast configuration");
              conf = new Config();
            }
          }

          // We have our own shutdown hook and need to ensure ours runs before Hazelcast is shutdown
          conf.setProperty("hazelcast.shutdownhook.enabled", "false");

          hazelcast = Hazelcast.newHazelcastInstance(conf);
        }

        Member localMember = hazelcast.getCluster().getLocalMember();
        nodeId = localMember.getUuid();
        localMember.setStringAttribute(NODE_ID_ATTRIBUTE, nodeId);
        membershipListenerId = hazelcast.getCluster().addMembershipListener(this);
        lifecycleListenerId = hazelcast.getLifecycleService().addLifecycleListener(this);

        subsMapHelper = new SubsMapHelper(vertx, hazelcast, nodeSelector);
        nodeInfoMap = hazelcast.getMap("__vertx.nodeInfo");

        prom.complete();
      }
    }, promise);
  }

  @Override
  public String getNodeId() {
    return nodeId;
  }

  @Override
  public List<String> getNodes() {
    List<String> list = new ArrayList<>();
    for (Member member : hazelcast.getCluster().getMembers()) {
      String nodeIdAttribute = member.getStringAttribute(NODE_ID_ATTRIBUTE);
      list.add(nodeIdAttribute != null ? nodeIdAttribute : member.getUuid());
    }
    return list;
  }

  @Override
  public void nodeListener(NodeListener listener) {
    this.nodeListener = listener;
  }

  @Override
  public void setNodeInfo(NodeInfo nodeInfo, Promise<Void> promise) {
    synchronized (this) {
      this.nodeInfo = nodeInfo;
    }
    HazelcastNodeInfo value = new HazelcastNodeInfo(nodeInfo);
    vertx.executeBlocking(prom -> {
      nodeInfoMap.put(nodeId, value);
      prom.complete();
    }, false, promise);
  }

  @Override
  public synchronized NodeInfo getNodeInfo() {
    return nodeInfo;
  }

  @Override
  public void getNodeInfo(String nodeId, Promise<NodeInfo> promise) {
    vertx.executeBlocking(prom -> {
      HazelcastNodeInfo value = nodeInfoMap.get(nodeId);
      if (value != null) {
        prom.complete(value.unwrap());
      } else {
        promise.fail("Not a member of the cluster");
      }
    }, false, promise);
  }

  @Override
  public <K, V> void getAsyncMap(String name, Promise<AsyncMap<K, V>> promise) {
    promise.complete(new HazelcastAsyncMap<>(vertx, hazelcast.getMap(name)));
  }

  @Override
  public <K, V> Map<K, V> getSyncMap(String name) {
    return hazelcast.getMap(name);
  }

  @Override
  public void getLockWithTimeout(String name, long timeout, Promise<Lock> promise) {
    vertx.executeBlocking(prom -> {
      ISemaphore iSemaphore = hazelcast.getSemaphore(LOCK_SEMAPHORE_PREFIX + name);
      boolean locked = false;
      long remaining = timeout;
      do {
        long start = System.nanoTime();
        try {
          locked = iSemaphore.tryAcquire(remaining, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          // OK continue
        }
        remaining = remaining - MILLISECONDS.convert(System.nanoTime() - start, NANOSECONDS);
      } while (!locked && remaining > 0);
      if (locked) {
        prom.complete(new HazelcastLock(iSemaphore, lockReleaseExec));
      } else {
        throw new VertxException("Timed out waiting to get lock " + name);
      }
    }, false, promise);
  }

  @Override
  public void getCounter(String name, Promise<Counter> promise) {
    promise.complete(new HazelcastCounter(vertx, hazelcast.getAtomicLong(name)));
  }

  @Override
  public void leave(Promise<Void> promise) {
    vertx.executeBlocking(prom -> {
      // We need to synchronized on the cluster manager instance to avoid other call to happen while leaving the
      // cluster, typically, memberRemoved and memberAdded
      synchronized (HazelcastClusterManager.this) {
        if (active) {
          try {
            active = false;
            lockReleaseExec.shutdown();
            subsMapHelper.close();
            boolean left = hazelcast.getCluster().removeMembershipListener(membershipListenerId);
            if (!left) {
              log.warn("No membership listener");
            }
            hazelcast.getLifecycleService().removeLifecycleListener(lifecycleListenerId);

            // Do not shutdown the cluster if we are not the owner.
            while (!customHazelcastCluster && hazelcast.getLifecycleService().isRunning()) {
              try {
                // This can sometimes throw java.util.concurrent.RejectedExecutionException so we retry.
                hazelcast.getLifecycleService().shutdown();
              } catch (RejectedExecutionException ignore) {
                log.debug("Rejected execution of the shutdown operation, retrying");
              }
              try {
                Thread.sleep(1);
              } catch (InterruptedException t) {
                // Manage the interruption in another handler.
                Thread.currentThread().interrupt();
              }
            }

            if (customHazelcastCluster) {
              hazelcast.getCluster().getLocalMember().removeAttribute(NODE_ID_ATTRIBUTE);
            }

          } catch (Throwable t) {
            prom.fail(t);
          }
        }
      }
      prom.complete();
    }, promise);
  }

  @Override
  public synchronized void memberAdded(MembershipEvent membershipEvent) {
    if (!active) {
      return;
    }
    Member member = membershipEvent.getMember();
    String attribute = member.getStringAttribute(NODE_ID_ATTRIBUTE);
    String nid = attribute != null ? attribute : member.getUuid();
    try {
      if (nodeListener != null) {
        nodeIds.add(nid);
        nodeListener.nodeAdded(nid);
      }
    } catch (Throwable t) {
      log.error("Failed to handle memberAdded", t);
    }
  }

  @Override
  public synchronized void memberRemoved(MembershipEvent membershipEvent) {
    if (!active) {
      return;
    }
    Member member = membershipEvent.getMember();
    String attribute = member.getStringAttribute(NODE_ID_ATTRIBUTE);
    String nid = attribute != null ? attribute : member.getUuid();
    try {
      membersRemoved(Collections.singleton(nid));
    } catch (Throwable t) {
      log.error("Failed to handle memberRemoved", t);
    }
  }

  private synchronized void membersRemoved(Set<String> ids) {
    cleanSubs(ids);
    cleanNodeInfos(ids);
    nodeInfoMap.put(nodeId, new HazelcastNodeInfo(getNodeInfo()));
    nodeSelector.registrationsLost();
    republishOwnSubs();
    if (nodeListener != null) {
      nodeIds.removeAll(ids);
      ids.forEach(nodeListener::nodeLeft);
    }
  }

  private void cleanSubs(Set<String> ids) {
    subsMapHelper.removeAllForNodes(ids);
  }

  private void cleanNodeInfos(Set<String> ids) {
    ids.forEach(nodeInfoMap::remove);
  }

  private void republishOwnSubs() {
    vertx.executeBlocking(prom -> {
      subsMapHelper.republishOwnSubs();
      prom.complete();
    }, false);
  }

  @Override
  public synchronized void stateChanged(LifecycleEvent lifecycleEvent) {
    if (!active) {
      return;
    }
    // Safeguard to make sure members list is OK after a partition merge
    if (lifecycleEvent.getState() == LifecycleEvent.LifecycleState.MERGED) {
      final List<String> currentNodes = getNodes();
      Set<String> newNodes = new HashSet<>(currentNodes);
      newNodes.removeAll(nodeIds);
      Set<String> removedMembers = new HashSet<>(nodeIds);
      removedMembers.removeAll(currentNodes);
      for (String nodeId : newNodes) {
        nodeListener.nodeAdded(nodeId);
      }
      membersRemoved(removedMembers);
      nodeIds.retainAll(currentNodes);
    }
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void addRegistration(String address, RegistrationInfo registrationInfo, Promise<Void> promise) {
    SubsOpSerializer serializer = SubsOpSerializer.get(vertx.getOrCreateContext());
    serializer.execute(subsMapHelper::put, address, registrationInfo, promise);
  }

  @Override
  public void removeRegistration(String address, RegistrationInfo registrationInfo, Promise<Void> promise) {
    SubsOpSerializer serializer = SubsOpSerializer.get(vertx.getOrCreateContext());
    serializer.execute(subsMapHelper::remove, address, registrationInfo, promise);
  }

  @Override
  public void getRegistrations(String address, Promise<List<RegistrationInfo>> promise) {
    vertx.executeBlocking(prom -> {
      prom.complete(subsMapHelper.get(address));
    }, false, promise);
  }

  @Override
  public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
  }

  /**
   * Get the Hazelcast config.
   *
   * @return a config object
   */
  public Config getConfig() {
    return conf;
  }

  /**
   * Set the Hazelcast config.
   *
   * @param config a config object
   */
  public void setConfig(Config config) {
    this.conf = config;
  }

  /**
   * Load Hazelcast config XML and transform it into a {@link Config} object.
   * The content is read from:
   * <ol>
   * <li>the location denoted by the {@code vertx.hazelcast.config} sysprop, if present, or</li>
   * <li>the {@code cluster.xml} file on the classpath, if present, or</li>
   * <li>the default config file</li>
   * </ol>
   * <p>
   * The cluster manager uses this method to load the config when the node joins the cluster, if no config was provided upon creation.
   * </p>
   * <p>
   * You may use this method to get a base config and customize it before the node joins the cluster.
   * In this case, don't forget to invoke {@link #setConfig(Config)} after you applied your changes.
   * </p>
   *
   * @return a config object
   */
  public Config loadConfig() {
    return ConfigUtil.loadConfig();
  }

  public HazelcastInstance getHazelcastInstance() {
    return hazelcast;
  }
}
