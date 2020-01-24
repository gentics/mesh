package com.gentics.mesh.plugin.formgenerator.service;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.formgenerator.FormGeneratorError;
import com.gentics.mesh.plugin.formgenerator.dagger.RootNodeUuids;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Single;

@Singleton
public class SchemaService {
	private final MeshRestClient adminClient;

	private final Map<String, String> rootNodeUuids;

	public static final String SCHEMA_NAME = "formgenerator";

	public static final String TEMPLATE_SCHEMA_NAME = "formgenerator_template";

	public static final String PLUGIN_SCHEMA_NAME = "formgenerator_plugin";

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
	public SchemaService(@AdminClient MeshRestClient adminClient, @RootNodeUuids Map<String, String> rootNodeUuids) {
		this.adminClient = adminClient;
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
		Single<Optional<SchemaResponse>> single = adminClient.findSchemas(new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).toSingle()
			.map(schemaListResponse -> {
				Optional<SchemaResponse> result = schemaListResponse.getData().stream().filter(schema -> name.equals(schema.getName())).findFirst()
					.map(e -> Optional.of(e)).orElse(Optional.empty());
				return result;
			});

		if (failIfNotFound) {
			return single.flatMap(schema -> {
				if (!schema.isPresent()) {
					String message = String.format("Schema with name \"%s\" could not be found.", name);
					return Single.error(new FormGeneratorError(404, message));
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
