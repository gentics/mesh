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

import com.hazelcast.core.*;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.spi.cluster.NodeSelector;
import io.vertx.core.spi.cluster.RegistrationInfo;
import io.vertx.core.spi.cluster.RegistrationUpdateEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Thomas Segismont
 */
public class SubsMapHelper implements EntryListener<String, HazelcastRegistrationInfo> {

  private static final Logger log = LoggerFactory.getLogger(SubsMapHelper.class);

  private final VertxInternal vertx;
  private final MultiMap<String, HazelcastRegistrationInfo> map;
  private final NodeSelector nodeSelector;
  private final String listenerId;

  private final ConcurrentMap<String, Set<RegistrationInfo>> ownSubs = new ConcurrentHashMap<>();
  private final ReadWriteLock republishLock = new ReentrantReadWriteLock();

  public SubsMapHelper(VertxInternal vertx, HazelcastInstance hazelcast, NodeSelector nodeSelector) {
    this.vertx = vertx;
    map = hazelcast.getMultiMap("__vertx.subs");
    this.nodeSelector = nodeSelector;
    listenerId = map.addEntryListener(this, false);
  }

  public List<RegistrationInfo> get(String address) {
    Lock readLock = republishLock.readLock();
    readLock.lock();
    try {
      List<RegistrationInfo> list = new ArrayList<>();
      for (HazelcastRegistrationInfo registrationInfo : map.get(address)) {
        list.add(registrationInfo.unwrap());
      }
      return list;
    } finally {
      readLock.unlock();
    }
  }

  public void put(String address, RegistrationInfo registrationInfo) {
    Lock readLock = republishLock.readLock();
    readLock.lock();
    try {
      ownSubs.compute(address, (add, curr) -> {
        Set<RegistrationInfo> res = curr != null ? curr : new CopyOnWriteArraySet<>();
        res.add(registrationInfo);
        return res;
      });
      map.put(address, new HazelcastRegistrationInfo(registrationInfo));
    } finally {
      readLock.unlock();
    }
  }

  public void remove(String address, RegistrationInfo registrationInfo) {
    Lock readLock = republishLock.readLock();
    readLock.lock();
    try {
      ownSubs.computeIfPresent(address, (add, curr) -> {
        curr.remove(registrationInfo);
        return curr.isEmpty() ? null : curr;
      });
      map.remove(address, new HazelcastRegistrationInfo(registrationInfo));
    } finally {
      readLock.unlock();
    }
  }

  public void removeAllForNodes(Set<String> nodeIds) {
    for (Map.Entry<String, HazelcastRegistrationInfo> entry : map.entrySet()) {
      HazelcastRegistrationInfo registrationInfo = entry.getValue();
      if (nodeIds.contains(registrationInfo.unwrap().nodeId())) {
        map.remove(entry.getKey(), registrationInfo);
      }
    }
  }

  public void republishOwnSubs() {
    Lock writeLock = republishLock.writeLock();
    writeLock.lock();
    try {
      for (Map.Entry<String, Set<RegistrationInfo>> entry : ownSubs.entrySet()) {
        String address = entry.getKey();
        for (RegistrationInfo registrationInfo : entry.getValue()) {
          map.put(address, new HazelcastRegistrationInfo(registrationInfo));
        }
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public void entryAdded(EntryEvent<String, HazelcastRegistrationInfo> event) {
    fireRegistrationUpdateEvent(event);
  }

  private void fireRegistrationUpdateEvent(EntryEvent<String, HazelcastRegistrationInfo> event) {
    String address = event.getKey();
    vertx.<List<RegistrationInfo>>executeBlocking(prom -> {
      prom.complete(get(address));
    }, false, ar -> {
      if (ar.succeeded()) {
        nodeSelector.registrationsUpdated(new RegistrationUpdateEvent(address, ar.result()));
      } else {
        log.trace("A failure occured while retrieving the updated registrations", ar.cause());
        nodeSelector.registrationsUpdated(new RegistrationUpdateEvent(address, Collections.emptyList()));
      }
    });
  }

  @Override
  public void entryEvicted(EntryEvent<String, HazelcastRegistrationInfo> event) {
  }

  @Override
  public void entryRemoved(EntryEvent<String, HazelcastRegistrationInfo> event) {
    fireRegistrationUpdateEvent(event);
  }

  @Override
  public void entryUpdated(EntryEvent<String, HazelcastRegistrationInfo> event) {
    fireRegistrationUpdateEvent(event);
  }

  @Override
  public void mapCleared(MapEvent event) {
  }

  @Override
  public void mapEvicted(MapEvent event) {
  }

  public void close() {
    map.removeEntryListener(listenerId);
  }
}
