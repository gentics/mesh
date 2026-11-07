package com.gentics.mesh.plugin.gcms;

import java.util.Collections;
import java.util.Set;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.gcms.config.GCMSPluginConfig;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

/**
 * The test plugin overrides the actual plugin class and overrides the {@link #getConfig()} method so that the config can be injected without the use of the
 * filesystem.
 */
public class GCMSTestPlugin extends GCMSPlugin {
	public static GCMSPluginConfig config;

	public static JsonObject publicKey;

	public GCMSTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public GCMSPluginConfig getConfig() {
		return config;
	}

	@Override
	public Single<GCMSPluginConfig> loadConfig(Class<GCMSPluginConfig> classOfT) {
		return Single.just(config);
	}

	@Override
	protected Completable loadPublicKey(GCMSPluginConfig config) {
		return Completable.complete();
	}

	@Override
	public Set<JsonObject> getPublicKeys() {
		return Collections.singleton(publicKey);
	}
}
