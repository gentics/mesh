package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.util.FormUtil;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

@Singleton
public class SchemaService {
	private static final Logger log = LoggerFactory.getLogger(SchemaService.class);

	private final MeshRestClient userClient;

	private final MeshRestClient adminClient;

	public static final String SCHEMA_NAME = "form";

	public static final String PLUGIN_SCHEMA_NAME = "forms_plugin";

	private final String schemaFieldsGraphQl;

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
	public SchemaService(@UserClient @Nullable MeshRestClient userClient, @AdminClient @Nullable MeshRestClient adminClient) {
		this.userClient = userClient;
		this.adminClient = adminClient;

		try {
			this.schemaFieldsGraphQl = IOUtils.toString(FormsPlugin.class.getResource("schemaFields.graphql"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

	/**
	 * Get the names of the fields in the schema for the given form
	 * @param projectName project name
	 * @param formUuid form uuid
	 * @return single list of field names
	 */
	public Single<List<String>> getSchemaFields(String projectName, String formUuid) {
		String schemaName = getSchemaName(formUuid);

		GraphQLRequest request = new GraphQLRequest()
				.setQuery(schemaFieldsGraphQl)
				.setVariables(new JsonObject().put("schemaName", schemaName));

		return adminClient.graphql(projectName, request).toSingle().map(response -> {
			if (CollectionUtils.isNotEmpty(response.getErrors())) {
				for (GraphQLError error : response.getErrors()) {
					log.warn("Error in graphql response {}", error.toJson());
				}
			}

			List<String> fieldNames = FormUtil.getFromResponse(response, "$.data.schema.fields[*].name");
			return fieldNames;
		});
	}
}
