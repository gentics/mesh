package com.gentics.mesh.liveness;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Implementation of the {@link LivenessManager}
 */
@Singleton
public class LivenessManagerImpl implements LivenessManager {
	private static final Logger log = LoggerFactory.getLogger(LivenessManagerImpl.class);

	private final Lazy<MeshPluginManager> pluginManager;

	protected boolean live = true;

	protected String error;

	protected final File liveFile;

	private ScheduledExecutorService executor;

	/**
	 * Create instance
	 * @param options mesh options
	 */
	@Inject
	public LivenessManagerImpl(MeshOptions options, Lazy<MeshPluginManager> pluginManager) {
		this.pluginManager = pluginManager;
		liveFile = new File(options.getLivePath());
		File liveFolder = liveFile.getParentFile();
		if (liveFolder != null && !liveFolder.exists() && !liveFolder.mkdirs()) {
			log.error("Could not create parent folder for livefile {" + liveFolder.getAbsolutePath() + "}");
		}
		try {
			liveFile.createNewFile();
		} catch (IOException e) {
			log.error("Could not create livefile {" + liveFile.getAbsolutePath() + "}", e);
		}
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleWithFixedDelay(() -> {
			if (isLive() && pluginsLive()) {
				// touch file
				liveFile.setLastModified(System.currentTimeMillis());
			}
		}, 10, 10, TimeUnit.SECONDS);
	}

	@Override
	public boolean isLive() {
		return live;
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
}
