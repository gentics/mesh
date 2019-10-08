package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

/**
 * 
 * @author jotschi
 *
 * @param <T>
 */
public abstract class AbstractCommercialPlugin<T> extends AbstractPlugin {

	private static final Logger log = LoggerFactory.getLogger(AbstractCommercialPlugin.class);

	private T config;

	public AbstractCommercialPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	public Single<T> loadConfig(Class<T> classOfT) {
		return Single.fromCallable(() -> {
			if (!getConfigFile().exists()) {
				T config = classOfT.newInstance();
				return writeConfig(config);
			} else {
				return readConfig(classOfT);
			}
		}).doOnSuccess(config -> {
			this.config = config;
			log.info("Loaded config {\n" + PluginConfigUtil.getYAMLMapper().writeValueAsString(config) + "\n}");
		});
	}

	public T getConfig() {
		return config;
	}

	public Vertx rxVertx() {
		return getRxVertx();
	}

}
