/*
 * Copyright 2020 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.hazelcast.hibernate.local;

import com.hazelcast.hibernate.shaded.caffeine.cache.Cache;
import com.hazelcast.hibernate.shaded.caffeine.cache.Policy;
import com.hazelcast.internal.util.MemoryInfoAccessor;
import com.hazelcast.internal.util.RuntimeMemoryInfoAccessor;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class FreeHeapBasedCacheEvictor implements AutoCloseable {
    private static final int TERMINATE_TIMEOUT_SECONDS = 5;
    private static final Duration DEFAULT_EVICTION_DELAY = Duration.ofSeconds(1);
    /**
     * This has been increase from originally 15 to 10000
     */
    private static final int EVICTION_BATCH_SIZE = 10000;
    private static final ILogger LOG = Logger.getLogger(FreeHeapBasedCacheEvictor.class);

    private final ScheduledExecutorService executorService;
    private final MemoryInfoAccessor memoryInfoAccessor;
    private final Duration evictionDelay;
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public FreeHeapBasedCacheEvictor() {
        this(newSingleThreadScheduledExecutor(defaultThreadFactory()), new RuntimeMemoryInfoAccessor(),
                DEFAULT_EVICTION_DELAY);
    }

    /**
     * just for testing
     */
    FreeHeapBasedCacheEvictor(ScheduledExecutorService executorService, MemoryInfoAccessor memoryInfoAccessor,
                              Duration evictionDelay) {
        this.executorService = executorService;
        this.memoryInfoAccessor = memoryInfoAccessor;
        this.evictionDelay = evictionDelay;
    }

    void start(String cacheName, Cache<?, ?> cache, long minimalHeapSizeInMB) {
        Policy.Eviction<?, ?> eviction = cache.policy().eviction()
                .orElseThrow(() -> new IllegalStateException("Eviction for cache '" + cacheName + "' not enabled"));
        LOG.info("Starting free-heap-size-based eviction of cache '" + cacheName + "'");
        ScheduledFuture<?> evictingTask = executorService.scheduleWithFixedDelay(() -> {
            if (freeHeapTooSmall(minimalHeapSizeInMB)) {
                eviction.coldest(EVICTION_BATCH_SIZE).forEach((key, value) -> cache.invalidate(key));
            }
        }, 0, evictionDelay.toMillis(), TimeUnit.MILLISECONDS);
        tasks.put(cacheName, evictingTask);
    }

    public void stop(String cacheName) {
        ScheduledFuture<?> task = tasks.remove(cacheName);
        if (task == null) {
            throw new IllegalStateException("Evicting task for cache '" + cacheName + "' not found");
        }
        task.cancel(false);
    }

    static ThreadFactory defaultThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return r -> new Thread(r, FreeHeapBasedCacheEvictor.class.getSimpleName() + "-free-heap-evictor-"
                + counter.getAndIncrement());
    }

    private boolean freeHeapTooSmall(long minimalHeapSizeInMB) {
        return availableMemoryInBytes() < minimalHeapSizeInMB;
    }

    private long availableMemoryInBytes() {
        return memoryInfoAccessor.getFreeMemory() + memoryInfoAccessor.getMaxMemory() - memoryInfoAccessor.getTotalMemory();
    }

    @Override
    public void close() {
        LOG.info("Shutting down " + FreeHeapBasedCacheEvictor.class.getSimpleName());
        executorService.shutdown();
        try {
            boolean success = executorService.awaitTermination(TERMINATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!success) {
                LOG.warning("ExecutorService awaitTermination could not completed gracefully in "
                        + TERMINATE_TIMEOUT_SECONDS + " seconds. Terminating forcefully.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warning("ExecutorService awaitTermination is interrupted. Terminating forcefully.", e);
            executorService.shutdownNow();
        }
        LOG.info("Shutdown of " + FreeHeapBasedCacheEvictor.class.getSimpleName() + " completed");
    }
}
