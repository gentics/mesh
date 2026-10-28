package com.gentics.mesh.plugin.like.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.like.dagger.AdminClient;
import com.gentics.mesh.plugin.like.dagger.RootNodeUuids;
import com.gentics.mesh.plugin.like.error.LikePluginError;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;

@Singleton
public class SchemaService {
	private final MeshRestClient adminClient;

	private final Map<String, String> rootNodeUuids;

	public static final String PLUGIN_SCHEMA_NAME = "like_plugin";

	public static final String SECTION_SCHEMA_NAME = "like_section";

	public static final String LIKE_SCHEMA_NAME = "like";

	@Inject
	public SchemaService(@AdminClient MeshRestClient adminClient, @RootNodeUuids Map<String, String> rootNodeUuids) {
		this.adminClient = adminClient;
		this.rootNodeUuids = rootNodeUuids;
	}

	public List<String> getSchemaNames() {
		return Arrays.asList(PLUGIN_SCHEMA_NAME, SECTION_SCHEMA_NAME, LIKE_SCHEMA_NAME);
	}

	/**
	 * Load a schema by name. If the schema does not exist, the behaviour depends on failIfNotFound
	 * 
	 * @param name
	 *            schema name
	 * @param failIfNotFound
	 *            true to return an error if the schema could not be found, otherwise the single will contain null
	 * @return single of the schema response which may contain the found schema within the optional
	 */
	public Single<Optional<SchemaResponse>> loadSchemaByName(String name, boolean failIfNotFound) {
		Single<Optional<SchemaResponse>> single = adminClient.findSchemas(new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)).toSingle()
			.map(schemaListResponse -> {
				Optional<SchemaResponse> result = schemaListResponse.getData().stream().filter(schema -> name.equals(schema.getName())).findFirst()
					.map(e -> Optional.of(e)).orElse(Optional.empty());
				return result;
			});

		if (failIfNotFound) {
			return single.flatMap(schema -> {
				if (!schema.isPresent()) {
					String message = String.format("Schema with name \"%s\" could not be found.", name);
					return Single.error(new LikePluginError(HttpResponseStatus.NOT_FOUND.code(), message));
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
