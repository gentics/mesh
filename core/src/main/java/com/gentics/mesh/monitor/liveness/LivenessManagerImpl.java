package com.gentics.mesh.monitor.liveness;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Implementation of the {@link LivenessManager}
 */
@Singleton
public class LivenessManagerImpl implements LivenessManager {
	private static final Logger log = LoggerFactory.getLogger(LivenessManagerImpl.class);

	protected boolean live = true;

	protected String error;

	protected final File liveFile;

	private ScheduledExecutorService executor;

	/**
	 * Create instance
	 * @param options mesh options
	 */
	@Inject
	public LivenessManagerImpl(MeshOptions options) {
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
			if (isLive()) {
				// touch file
				liveFile.setLastModified(System.currentTimeMillis());
			}
		}, 10, 10, TimeUnit.SECONDS);
	}

	@Override
	public boolean isLive() {
		return true;
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
}
