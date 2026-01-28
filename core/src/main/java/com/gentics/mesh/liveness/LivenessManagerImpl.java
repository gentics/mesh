package com.gentics.mesh.liveness;

import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import dagger.Lazy;

/**
 * Implementation of the {@link LivenessManager}
 */
@Singleton
public class LivenessManagerImpl implements LivenessManager {
	private static final Logger log = LoggerFactory.getLogger(LivenessManagerImpl.class);

	private final Lazy<MeshPluginManager> pluginManager;

	protected boolean live = true;

	protected boolean memoryOk = true;

	protected String error;

	protected final File liveFile;

	private ScheduledExecutorService executor;

	private final long memoryLimit;

	private final long gcTimeLimit;

	private long lastTimestampMs;

	private long lastTotalCollectionTimeMs;

	private long lastTotalCollectionCount;

	/**
	 * Create instance
	 * @param options mesh options
	 */
	@Inject
	public LivenessManagerImpl(MeshOptions options, Lazy<MeshPluginManager> pluginManager) {
		this.pluginManager = pluginManager;
		liveFile = new File(options.getLivePath());
		this.memoryLimit = options.getMonitoringOptions().getMemoryLimit();
		this.gcTimeLimit = options.getMonitoringOptions().getGcTimeLimit();
		File liveFolder = liveFile.getParentFile();
		if (liveFolder != null && !liveFolder.exists() && !liveFolder.mkdirs()) {
			log.warn("Could not create parent folder for livefile {" + liveFolder.getAbsolutePath() + "}");
		}
		try {
			liveFile.createNewFile();
		} catch (IOException e) {
			log.error("Could not create livefile {" + liveFile.getAbsolutePath() + "}", e);
		}
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleWithFixedDelay(() -> {
			doMemoryChecks();
			if (isLive() && pluginsLive()) {
				// touch file
				liveFile.setLastModified(System.currentTimeMillis());
			}
		}, 10, 10, TimeUnit.SECONDS);
	}

	@Override
	public boolean isLive() {
		return live && memoryOk;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public void setLive(boolean live, String error) {
		this.live = live;
		this.error = error;
	}

	@Override
	public void shutdown() {
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}
		liveFile.delete();
	}

	/**
	 * Check whether the plugins are live (no plugin is in status FAILED)
	 * @return true, iff plugins are live
	 */
	protected boolean pluginsLive() {
		for (String id : pluginManager.get().getPluginIds()) {
			PluginStatus status = pluginManager.get().getStatus(id);
			if (status == PluginStatus.FAILED) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get the amount of used memory (percent of max available memory) and amount of time spent in garbage collections (since the last measurement).
	 * If both amounts exceed the configured limits, set the flag {@link #memoryOk} to false.
	 * If both amounts are below the limits, set the flag {@link #memoryOk} to true.
	 * If only one amount exceeds the limit, leave the flag {@link #memoryOk} in its current state.
	 */
	protected void doMemoryChecks() {
		if (memoryLimit <= 0 || gcTimeLimit <= 0) {
			return;
		}
		long timestamp = System.currentTimeMillis();

		Runtime runtime = Runtime.getRuntime();
		long free = runtime.freeMemory();
		long total = runtime.totalMemory();
		long max = runtime.maxMemory();
		long used = total - free;
		float percUsed = (used * 100f / max);

		long totalCollectionCount = 0;
		long totalCollectionTimeMs = 0;

		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			totalCollectionCount += gc.getCollectionCount();
			totalCollectionTimeMs += gc.getCollectionTime();
		}

		String gcInfo = "";
		if (lastTimestampMs > 0) {
			long timeSpentForCollectionsMs = totalCollectionTimeMs - lastTotalCollectionTimeMs;
			long time = timestamp - lastTimestampMs;
			float percCollectionTime = (timeSpentForCollectionsMs * 100f / time);

			if (percUsed > memoryLimit && percCollectionTime > gcTimeLimit) {
				memoryOk = false;
			} else if (percUsed <= memoryLimit && percCollectionTime <= gcTimeLimit) {
				memoryOk = true;
			}

			gcInfo = ", time spent for GCs: %.2f%% - limit is %d%%".formatted(percCollectionTime, gcTimeLimit);
		} else {
			memoryOk = true;
		}
		lastTimestampMs = timestamp;
		lastTotalCollectionCount = totalCollectionCount;
		lastTotalCollectionTimeMs = totalCollectionTimeMs;

		String message = "Memory %s. Used: %s/%s (%.2f%% - limit is %d%%)%s".formatted(memoryOk ? "OK" : "NOT OK",
				mb(used), mb(max), percUsed, memoryLimit, gcInfo);
		if (memoryOk) {
			log.info(message);
		} else {
			log.error(message);
		}
	}

	/**
	 * Format the given number of bytes as megabytes
	 * @param bytes bytes
	 * @return bytes formatted as megabytes
	 */
	protected String mb(long bytes) {
		return "%sM".formatted(bytes / (1024*1024));
	}
}
