package com.gentics.mesh.plugin.forms.service;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Single;

@Singleton
public class SchemaService {
	private final MeshRestClient userClient;

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
	public SchemaService(@UserClient @Nullable MeshRestClient userClient) {
		this.userClient = userClient;
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
		Single<Optional<SchemaResponse>> single = userClient.findSchemas(new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)).toSingle()
			.map(schemaListResponse -> {
				Optional<SchemaResponse> result = schemaListResponse.getData().stream().filter(schema -> name.equals(schema.getName())).findFirst()
					.map(e -> Optional.of(e)).orElse(Optional.empty());
				return result;
			});

		if (failIfNotFound) {
			return single.flatMap(schema -> {
				if (!schema.isPresent()) {
					String message = String.format("Schema with name \"%s\" could not be found.", name);
					return Single.error(new FormsError(message, 404));
				} else {
					return Single.just(schema);
				}
			});
		} else {
			return single;
		}
	}
}
