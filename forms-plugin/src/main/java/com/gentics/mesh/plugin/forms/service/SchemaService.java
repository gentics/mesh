package com.gentics.mesh.plugin.forms.service;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.dagger.RootNodeUuids;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Single;

@Singleton
public class SchemaService {
	private final MeshRestClient userClient;

	private final Map<String, String> rootNodeUuids;

	public static final String SCHEMA_NAME = "form";

	public static final String PLUGIN_SCHEMA_NAME = "forms_plugin";

	/**
	 * Get the schema name for the form uuid
	 * 
	 * @param formUuid
	 *            form uuid
	 * @return schema name
	 */
	public static String getSchemaName(String formUuid) {
		return String.format("%s_%s", SCHEMA_NAME, formUuid);
	}

	@Inject
	public SchemaService(@UserClient @Nullable MeshRestClient userClient, @RootNodeUuids Map<String, String> rootNodeUuids) {
		this.userClient = userClient;
		this.rootNodeUuids = rootNodeUuids;
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
		Single<Optional<SchemaResponse>> single = userClient.findSchemas(new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).toSingle()
			.map(schemaListResponse -> {
				Optional<SchemaResponse> result = schemaListResponse.getData().stream().filter(schema -> name.equals(schema.getName())).findFirst()
					.map(e -> Optional.of(e)).orElse(Optional.empty());
				return result;
			});

		if (failIfNotFound) {
			return single.flatMap(schema -> {
				if (!schema.isPresent()) {
					String message = String.format("Schema with name \"%s\" could not be found.", name);
					return Single.error(new FormsError(404, message));
				} else {
					return Single.just(schema);
				}
			});
		} else {
			return single;
		}
	}

	/**
	 * Set the uuid of the FormsPlugin Root node for the project
	 * @param projectName project name
	 * @param uuid root node uuid
	 */
	public void setRootNodeUuid(String projectName, String uuid) {
		rootNodeUuids.put(projectName, uuid);
	}

	/**
	 * Get the uuid of the FormsPlugin Root node for the project
	 * @param projectName project name
	 * @return uuid or null, if not found
	 */
	public String getRootNodeUuid(String projectName) {
		return rootNodeUuids.get(projectName);
	}
}
