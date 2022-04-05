package com.gentics.mesh.plugin.export.util;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.export.extension.ConfigValueProvider;
import com.gentics.mesh.plugin.export.extension.ExportConfigValuePlugin;
import com.gentics.mesh.plugin.export.model.PluginConfiguration;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;

@Singleton
public class MappingService {

	private static final Logger log = LoggerFactory.getLogger(MappingService.class);

	private final PluginManager manager;
	private final PluginConfiguration config;
	private final ObjectCache<Map<String, Map<String, String>>> cache;

	@Inject
	public MappingService(PluginManager manager, PluginConfiguration config) {
		this.manager = manager;
		this.config = config;
		this.cache = new ObjectCache<>(config.getCacheTime());
	}

	public Single<Map<String, Map<String, String>>> getConfigValues(String projectName) {
		Map<String, ConfigValueProvider> providers = new HashMap<>();

		this.manager.getPlugins().stream()
			.filter(wrapper -> wrapper.getPlugin() instanceof ExportConfigValuePlugin && wrapper.getPluginState() == PluginState.STARTED)
			.forEach(wrapper -> {
				ExportConfigValuePlugin plugin = (ExportConfigValuePlugin) wrapper.getPlugin();
				for (ConfigValueProvider provider : plugin.getConfigValueProviders()) {
					if (provider != null) {
						providers.put(projectName + ":" + provider.getClass().getCanonicalName(), provider);
					}
				}
			});

		return Observable.fromIterable(providers.entrySet())
			.flatMapSingle(entry -> this.cache.computeIfAbsent(
				entry.getKey(),
				() -> entry.getValue().getConfigValues(projectName),
				err -> {
					log.warn(
						"Provider '{}' had an error! Falling back to empty value for now. {}",
						entry.getValue().getClass().getCanonicalName(),
						err
					);
					return new HashMap<>();
				}
			))
			.toList()
			.map(Utils::mergeMappings);
	}
}
