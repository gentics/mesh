package com.gentics.mesh.plugin.export;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.export.extension.ConfigValueProvider;
import com.gentics.mesh.plugin.export.model.PluginConfiguration;
import com.gentics.mesh.plugin.export.model.WebrootPathConfiguration;
import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.pf4j.Extension;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Extension
@Singleton
public class WebrootConfigValueProvider implements ConfigValueProvider {

	private static final Logger log = LoggerFactory.getLogger(WebrootConfigValueProvider.class);

	private final MeshRestClient client;
	private final PluginConfiguration config;

	@Inject
	public WebrootConfigValueProvider(MeshRestClient client, PluginConfiguration config) {
		this.client = client;
		this.config = config;
	}

	@Override
	public Single<Map<String, Map<String, String>>> getConfigValues(String projectName) {
		return Observable.fromIterable(config.getWebrootPaths())
			.filter(setting -> setting.getProjects() == null || setting.getProjects().isEmpty() || setting.getProjects().contains(projectName))
			.map(WebrootPathConfiguration::getPath)
			.map(Utils::stripSlashes)
			.flatMapSingle(path -> this.retrieveConfigNodeFromPath(projectName, "/" + path))
			.map(Utils::getMappingFromNode)
			.toList()
			.map(Utils::mergeMappings);
	}

	private Single<NodeResponse> retrieveConfigNodeFromPath(String projectName, String path) {
		Observable<MeshWebrootResponse> obs;
		try {
			obs = this.client.webroot(projectName, path).toObservable();
		} catch (Exception e) {
			obs = Observable.error(e);
		}

		return obs
			.doAfterNext(MeshWebrootResponse::close)
			.filter(response -> !response.isBinary())
			.firstOrError()
			.map(MeshWebrootResponse::getNodeResponse)
			.doOnSuccess(node -> log.info("Fetched config from webroot in project '{}' with path '{}'!", projectName, path))
			.doOnError(err -> log.warn("Error encountered when retrieving config node", err));
	}
}
