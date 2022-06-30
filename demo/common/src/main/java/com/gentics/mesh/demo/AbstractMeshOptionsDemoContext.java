package com.gentics.mesh.demo;

import java.io.File;

import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.env.MeshOptionsContext;

/**
 * Options holder impl for {@link MeshDemoOptionsProvider}
 * 
 * @author plyhun
 *
 * @param <T> options type
 */
public abstract class AbstractMeshOptionsDemoContext<T extends MeshOptions> implements MeshOptionsContext<T>, MeshDemoOptionsProvider<T> {

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
	}

	private final T options;

	public AbstractMeshOptionsDemoContext(String[] args, Class<? extends T> optionsClass) {
		options = OptionsLoader.createOrloadOptions(optionsClass, args);
	}

	@Override
	public T getOptions() {
		return options;
	}

	@Override
	public void setupOptions(T options) {
		// None required for Runner
	}
}
