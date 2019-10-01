package com.gentics.mesh.plugin.comment;

import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.comment.model.CommentPluginSchema;
import com.gentics.mesh.plugin.comment.service.SchemaService;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.InputStream;

import static com.gentics.mesh.plugin.comment.util.BasicUtil.propagateError;

@Singleton
public class Resources {

	private final SchemaService schemaService;

	@Inject
	public Resources(SchemaService schemaService) {
		this.schemaService = schemaService;
	}

	/**
	 * Loads a schema from the classpath as a {@link SchemaCreateRequest}.
	 */
	public SchemaCreateRequest loadSchema(CommentPluginSchema schema) {
		String resourcePath = String.format("/schemas/%s.json", schema.getName());
		SchemaCreateRequest request = JsonUtil.readValue(loadResource(resourcePath), SchemaCreateRequest.class);
		request.setName(schemaService.getMeshSchemaName(schema));
		return request;
	}

	/**
	 * Loads a GraphQL query from the classpath
	 *
	 * @param name Name of the query, without folder or extension
	 */
	public String loadQuery(String name) {
		return loadResource(String.format("/queries/%s.gql", name));
	}

	/**
	 * Loads a resource as String from the classpath
	 *
	 * @param name Path of the resource
	 */
	private String loadResource(String name) {
		InputStream stream = getClass().getResourceAsStream(name);

		return propagateError(() -> IOUtils.toString(stream, Charsets.UTF_8));
	}
}
