package com.gentics.mesh.plugin.favorite;

import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.plugin.favorite.client.AdminClient;
import com.gentics.mesh.plugin.favorite.dagger.RootNodeUuids;
import com.gentics.mesh.plugin.favorite.error.FavoritePluginError;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class SchemaService {
	private final MeshRestClient adminClient;

	private final Map<String, String> rootNodeUuids;

	public static final String PLUGIN_SCHEMA_NAME = "folder";

	public static final String SECTION_SCHEMA_NAME = "folder";

	public static final String FAVORITE_SCHEMA_NAME = "favorite";


	@Inject
	public SchemaService(@AdminClient MeshRestClient adminClient, @RootNodeUuids Map<String, String> rootNodeUuids) {
		this.adminClient = adminClient;
		this.rootNodeUuids = rootNodeUuids;
	}

	public List<String> getSchemaNames() {
		return Arrays.asList(FAVORITE_SCHEMA_NAME);
	}

	/**
	 * Load a schema by name. If the schema does not exist, the behaviour depends on failIfNotFound
	 *
	 * @param name           schema name
	 * @param failIfNotFound true to return an error if the schema could not be found, otherwise the single will contain
	 *                       null
	 * @return single of the schema response which may contain the found schema within the optional
	 */
	public Single<Optional<SchemaResponse>> loadSchemaByName(String name, boolean failIfNotFound) {
		Single<Optional<SchemaResponse>> single = adminClient.findSchemas().toSingle()
				.map(schemaListResponse -> {
					Optional<SchemaResponse> result = schemaListResponse.getData().stream().filter(schema -> name.equals(schema.getName())).findFirst()
							.map(e -> Optional.of(e)).orElse(Optional.empty());
					return result;
				});

		if (failIfNotFound) {
			return single.flatMap(schema -> {
				if (!schema.isPresent()) {
					return Single.error(new FavoritePluginError(String.format("Schema with name \"%s\" could not be found.", name)));
				} else {
					return Single.just(schema);
				}
			});
		} else {
			return single;
		}
	}

	public void setRootNodeUuid(String projectName, String uuid) {
		rootNodeUuids.put(projectName, uuid);
	}

	public String getRootNodeUuid(String projectName) {
		return rootNodeUuids.get(projectName);
	}
}
